package com.nyyb.nyybserver.routine.service;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.routine.data.dto.request.SaveRoutineRequestDto;
import com.nyyb.nyybserver.routine.data.dto.response.CreateRoutineResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.LlmRoutineItemDto;
import com.nyyb.nyybserver.routine.data.dto.response.LlmRoutineResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDayProductDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDayResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineProductDto;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemSelectionRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineService {

    private final ChatClient routineChatClient;
    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;
    private final RoutineItemSelectionRepository routineItemSelectionRepository;
    private final ProductIngredientRepository productIngredientRepository;

    /**
     * routineId의 루틴(analyze 단계에서 생성) -> LLM 루틴 설계 -> Routine·RoutineItem 갱신 -> 오전/오후 루틴 반환
     * @param routineId analyze 응답으로 받은 루틴 식별자
     * @return CreateRoutineResponseDto (score·scoreReason·summary + 오전/오후 루틴 제품)
     */
    @Transactional
    public CreateRoutineResponseDto createRoutine(UUID routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routineId: " + routineId));

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routineId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("해당 routineId의 루틴 아이템이 없습니다: " + routineId);
        }

        List<Product> products = items.stream().map(RoutineItem::getProduct).toList();

        String userMessage = buildUserMessage(products);
        log.info("OpenAI 루틴 요청 메시지:\n{}", userMessage);

        // LLM 호출 + 구조화 출력(JSON -> DTO)
        LlmRoutineResponseDto llmResponse = routineChatClient.prompt()
                .user(userMessage)
                .call()
                .entity(LlmRoutineResponseDto.class);

        log.info("OpenAI 루틴 응답:\n{}", llmResponse);

        // 루틴 평가 결과 반영
        routine.applyLlmRoutine(llmResponse.score(), llmResponse.scoreReason(), llmResponse.summary());
        routineRepository.save(routine);

        // productId -> RoutineItem 매핑 후 LLM 슬롯/추천 반영
        Map<Long, RoutineItem> itemMap = items.stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (LlmRoutineItemDto dto : llmResponse.items()) {
            RoutineItem item = itemMap.get(dto.productId());
            if (item == null) {
                throw new IllegalArgumentException("루틴에 없는 productId: " + dto.productId());
            }
            // LLM이 내려준 슬롯/추천(KEEP·REMOVE)/이유를 저장
            item.applyLlmRoutine(dto.slot(), dto.recommended(), dto.recommendReason());
        }
        routineItemRepository.saveAll(items);

        // 사용자가 고른 슬롯(userRoutineSlot) 기준으로 오전/오후 루틴 분리 (BOTH는 양쪽 모두 포함)
        List<RoutineProductDto> morning = filterByUserSlot(items, RoutineSlot.MORNING);
        List<RoutineProductDto> evening = filterByUserSlot(items, RoutineSlot.EVENING);

        return new CreateRoutineResponseDto(
                routine.getId(),
                llmResponse.score(),
                llmResponse.scoreReason(),
                llmResponse.summary(),
                morning,
                evening
        );
    }

    /**
     * routineId + 시간대(morning/evening) -> 해당 시간대 제품 목록 반환
     * - day 소속: userRoutineSlot 이 요청 시간대(또는 BOTH=전체는 양쪽 모두 포함)
     * - 각 제품 recommended: LLM 추천 슬롯(llmRoutineSlot)이 이 시간대를 커버하고 REMOVE일 때만 REMOVE, 그 외엔 KEEP
     * @param routineId 루틴 식별자
     * @param slot      MORNING(아침) 또는 EVENING(저녁)
     */
    @Transactional(readOnly = true)
    public RoutineDayResponseDto getRoutineDay(UUID routineId, RoutineSlot slot) {
        routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routineId: " + routineId));

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routineId);

        List<RoutineDayProductDto> products = items.stream()
                .filter(item -> matchesDay(item.getUserRoutineSlot(), slot))
                .map(item -> toDayProduct(item, slot))
                .toList();

        return new RoutineDayResponseDto(slot, products);
    }

    // 요청 슬롯(daySlot) 기준으로 제품을 DTO로 가공.
    // 슬롯은 응답 최상단 slot에만 있고, 제품에는 KEEP/REMOVE만 담는다.
    // - 유지(KEEP) 제품: 이 제품이 뜨는 day라면 항상 KEEP + 유지 이유를 그대로 노출한다.
    // - 제외(REMOVE) 제품: LLM 추천 슬롯이 이 day를 커버(daySlot 또는 BOTH)할 때만 REMOVE + 제외 이유를 노출하고,
    //   커버하지 않는 day에서는 KEEP으로 낮추며, 제외 이유는 배지와 어긋나므로 싣지 않는다.
    private RoutineDayProductDto toDayProduct(RoutineItem item, RoutineSlot daySlot) {
        Product product = item.getProduct();

        boolean isRemove = item.getRecommended() == RecommendStatus.REMOVE;
        boolean removeInThisDay = isRemove && matchesDay(item.getLlmRoutineSlot(), daySlot);

        RecommendStatus recommended = removeInThisDay ? RecommendStatus.REMOVE : RecommendStatus.KEEP;
        String recommendReason = isRemove
                ? (removeInThisDay ? item.getRecommendReason() : null) // 제외 제품: 제외가 적용되는 day에서만 이유 노출
                : item.getRecommendReason();                           // 유지 제품: 항상 유지 이유 노출

        return new RoutineDayProductDto(
                product.getId(),
                product.getCategory(),
                product.getProductName(),
                recommended,
                recommendReason
        );
    }

    // 슬롯이 요청 시간대에 해당하는지 (요청 슬롯 또는 BOTH=전체). null이면 미해당.
    private boolean matchesDay(RoutineSlot itemSlot, RoutineSlot target) {
        return itemSlot == target || itemSlot == RoutineSlot.BOTH;
    }

    /**
     * routineId + 제품별 유저 선택(slot + KEEP/REMOVE) -> (routineId, productId, slot) 단위로 슬롯 선택 upsert
     * + REMOVE인 슬롯 레코드 수를 Routine.afterCount에 저장.
     * 오전/저녁 화면이 독립적으로 저장돼도 각자 자기 슬롯 레코드만 갱신하므로 병합 로직이 필요 없다.
     * BOTH 제품은 슬롯 레코드가 2개(오전·저녁)라 양쪽 제거 시 자동으로 2, 한쪽만 제거하면 1이 된다.
     * @param routineId 루틴 식별자
     * @param request   product id + slot + action 목록
     * @return routineId
     */
    @Transactional
    public UUID saveRoutine(UUID routineId, SaveRoutineRequestDto request) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routineId: " + routineId));

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routineId);
        Map<Long, RoutineItem> itemMap = items.stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (SaveRoutineRequestDto.ProductSelection ps : request.getProducts()) {
            RoutineItem item = itemMap.get(ps.getId());
            if (item == null) {
                throw new IllegalArgumentException("루틴에 없는 productId: " + ps.getId());
            }
            item.applyUserSelection(ps.getSlot(), ps.getAction());
        }
        routineItemRepository.saveAll(items);

        // 제거(REMOVE)로 선택된 슬롯 레코드 수 -> afterCount (BOTH 양쪽 제거 시 자동 2)
        long afterCount = routineItemSelectionRepository.countByRoutineIdAndAction(routineId, RecommendStatus.REMOVE);
        routine.applyAfterCount((int) afterCount);
        routineRepository.save(routine);

        return routine.getId();
    }

    // 사용자가 고른 슬롯(userRoutineSlot)이 해당 시간대(또는 BOTH)인 제품만 골라 id + category + productName로 변환
    private List<RoutineProductDto> filterByUserSlot(List<RoutineItem> items, RoutineSlot slot) {
        return items.stream()
                .filter(item -> matchesDay(item.getUserRoutineSlot(), slot))
                .map(item -> {
                    Product product = item.getProduct();
                    return new RoutineProductDto(product.getId(), product.getCategory(), product.getProductName());
                })
                .toList();
    }

    // 제품별 productId + productName + recommended + recommendReason + 성분 -> 프롬프트 텍스트로 조립
    private String buildUserMessage(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 제품들로 스킨케어 루틴을 설계해 주세요.\n\n");

        for (Product product : products) {
            sb.append("=== productId: ").append(product.getId()).append(" ===\n");
            sb.append("productName: ").append(product.getProductName()).append("\n");
            sb.append("category: ").append(product.getCategory().name()).append("\n");
            sb.append("recommended: ").append(product.getRecommended()).append("\n");
            sb.append("recommendReason: ").append(product.getRecommendReason()).append("\n");
            sb.append("ingredients: ").append(formatIngredients(product.getId())).append("\n\n");
        }

        return sb.toString();
    }

    // 해당 제품의 성분을 "성분명(위험도) - 설명" 형태로 나열
    private String formatIngredients(Long productId) {
        List<ProductIngredient> productIngredients =
                productIngredientRepository.findByProductIdWithIngredient(productId);

        if (productIngredients.isEmpty()) {
            return "(인식된 성분 없음)";
        }

        return productIngredients.stream()
                .map(this::formatIngredient)
                .reduce((a, b) -> a + "\n  - " + b)
                .map(s -> "\n  - " + s)
                .orElse("(인식된 성분 없음)");
    }

    private String formatIngredient(ProductIngredient pi) {
        Ingredient ingredient = pi.getIngredient();
        // 마스터 매칭 실패 시 ingredient == null → OCR 원문(rawName) 사용
        if (ingredient == null) {
            return pi.getRawName();
        }
        String risk = ingredient.getRiskLevel() != null ? ingredient.getRiskLevel().name() : "UNKNOWN";
        String toxic = Boolean.TRUE.equals(ingredient.getIsToxic()) ? ", 독성" : "";
        String description = ingredient.getDescription() != null ? " - " + ingredient.getDescription() : "";
        return ingredient.getName() + "(" + risk + toxic + ")" + description;
    }
}
