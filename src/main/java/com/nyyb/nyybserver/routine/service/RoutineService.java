package com.nyyb.nyybserver.routine.service;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.routine.data.dto.request.RoutineSaveRequestDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDesignResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.LlmRoutineItemDto;
import com.nyyb.nyybserver.routine.data.dto.response.LlmRoutineResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDayProductDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDayResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineProductDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineSummaryDto;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.exception.RoutineNotFoundException;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemSelectionRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int NOON_HOUR = 12; // 하루를 반으로 나누는 기준 (오전/오후)

    private final ChatClient routineChatClient;
    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;
    private final RoutineItemSelectionRepository routineItemSelectionRepository;
    private final ProductIngredientRepository productIngredientRepository;

    /**
     * routineId의 루틴(analyze 단계에서 생성) -> LLM 루틴 설계 -> Routine·RoutineItem 갱신 -> 오전/오후 루틴 반환
     * @param routineId analyze 응답으로 받은 루틴 식별자
     * @param userId    소유자 id (본인 루틴만 설계 가능)
     * @return RoutineDesignResponseDto (score·scoreReason·summary + 오전/오후 루틴 제품)
     * @throws RoutineNotFoundException 해당 id의 루틴이 없거나 본인 소유가 아닌 경우
     */
    @Transactional
    public RoutineDesignResponseDto designRoutine(UUID routineId, Long userId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

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

        return new RoutineDesignResponseDto(
                routine.getId(),
                routine.getTitle(),
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
     * @param userId    소유자 id (본인 루틴만 조회 가능)
     * @param slot      MORNING(아침) 또는 EVENING(저녁)
     * @throws RoutineNotFoundException 해당 id의 루틴이 없거나 본인 소유가 아닌 경우
     */
    @Transactional(readOnly = true)
    public RoutineDayResponseDto getRoutineDay(UUID routineId, Long userId, RoutineSlot slot) {
        routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routineId);

        List<RoutineDayProductDto> products = items.stream()
                .filter(item -> matchesDay(item.getUserRoutineSlot(), slot))
                .map(item -> toDayProduct(item, slot))
                .toList();

        return new RoutineDayResponseDto(slot, products);
    }

    /**
     * 루틴 상세 조회. 저장된 Routine·RoutineItem으로 designRoutine과 동일한 응답을 재구성한다.
     * (설계 전 루틴이면 score·scoreReason·summary가 null)
     * @param routineId 루틴 식별자
     * @param userId    소유자 id (본인 루틴만 조회 가능)
     * @return RoutineDesignResponseDto (score·scoreReason·summary + 오전/오후 루틴 제품)
     * @throws RoutineNotFoundException 해당 id의 루틴이 없거나 본인 소유가 아닌 경우
     */
    @Transactional(readOnly = true)
    public RoutineDesignResponseDto getRoutine(UUID routineId, Long userId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routineId);

        // 사용자가 고른 슬롯(userRoutineSlot) 기준으로 오전/오후 루틴 분리 (BOTH는 양쪽 모두 포함)
        return new RoutineDesignResponseDto(
                routine.getId(),
                routine.getTitle(),
                routine.getScore(),
                routine.getScoreReason(),
                routine.getSummary(),
                filterByUserSlot(items, RoutineSlot.MORNING),
                filterByUserSlot(items, RoutineSlot.EVENING)
        );
    }

    /**
     * 현재 로그인 유저의 가장 최신 루틴을 상세 조회 형식으로 반환.
     * 대한민국 시간(KST) 기준 오전이면 MORNING, 오후면 EVENING 슬롯만 채우고 반대쪽은 빈 목록으로 내려간다.
     * (BOTH 제품은 filterByUserSlot에서 양쪽 모두에 포함되므로 어느 시간대든 함께 노출된다)
     * @param userId 소유자 id
     * @return RoutineDesignResponseDto (현재 시간대 슬롯만 채워진 상세 응답)
     * @throws RoutineNotFoundException 해당 유저의 루틴이 하나도 없는 경우
     */
    @Transactional(readOnly = true)
    public RoutineDesignResponseDto getLatestRoutine(Long userId) {
        Routine routine = routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .orElseThrow(RoutineNotFoundException::new);

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routine.getId());
        RoutineSlot currentSlot = currentSlot();

        return new RoutineDesignResponseDto(
                routine.getId(),
                routine.getTitle(),
                routine.getScore(),
                routine.getScoreReason(),
                routine.getSummary(),
                currentSlot == RoutineSlot.MORNING ? filterByUserSlot(items, RoutineSlot.MORNING) : List.of(),
                currentSlot == RoutineSlot.EVENING ? filterByUserSlot(items, RoutineSlot.EVENING) : List.of()
        );
    }

    /**
     * 루틴 삭제. 실제 행을 지우지 않고 소유자만 해제해 유저의 목록·상세에서 사라지게 한다.
     * (루틴 아이템·유저 선택 등 참조 데이터를 그대로 살려두기 위해 소프트 삭제로 처리)
     * @param routineId 삭제할 루틴 id
     * @param userId    소유자 id (본인 루틴만 삭제 가능)
     * @throws RoutineNotFoundException 해당 id의 루틴이 없거나 본인 소유가 아닌 경우
     */
    @Transactional
    public void deleteRoutine(UUID routineId, Long userId) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        routine.releaseOwner();
    }

    // 하루를 반으로 나눠 KST 12시 이전이면 MORNING, 이후면 EVENING
    private RoutineSlot currentSlot() {
        return LocalTime.now(KOREA_ZONE).getHour() < NOON_HOUR ? RoutineSlot.MORNING : RoutineSlot.EVENING;
    }

    /**
     * 현재 로그인 유저(게스트/카카오 공통)의 루틴 목록을 최신순으로 페이징 조회
     * @param userId   소유자 id
     * @param pageable page/size 페이징 정보
     * @return id + title 목록
     */
    @Transactional(readOnly = true)
    public List<RoutineSummaryDto> getRoutines(Long userId, Pageable pageable) {
        return routineRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable).stream()
                .map(RoutineSummaryDto::from)
                .toList();
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
     * + KEEP인 슬롯 레코드 수를 Routine.afterCount에 저장.
     * 오전/저녁 화면이 독립적으로 저장돼도 각자 자기 슬롯 레코드만 갱신하므로 병합 로직이 필요 없다.
     * BOTH 제품은 슬롯 레코드가 2개(오전·저녁)라 양쪽 유지 시 자동으로 2, 한쪽만 유지하면 1이 된다.
     * @param routineId 루틴 식별자
     * @param userId    소유자 id (본인 루틴만 저장 가능)
     * @param request   product id + slot + action 목록
     * @return routineId
     * @throws RoutineNotFoundException 해당 id의 루틴이 없거나 본인 소유가 아닌 경우
     */
    @Transactional
    public UUID saveRoutine(UUID routineId, Long userId, RoutineSaveRequestDto request) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        List<RoutineItem> items = routineItemRepository.findByRoutineIdWithProduct(routineId);
        Map<Long, RoutineItem> itemMap = items.stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (RoutineSaveRequestDto.ProductSelection ps : request.getProducts()) {
            RoutineItem item = itemMap.get(ps.getId());
            if (item == null) {
                throw new IllegalArgumentException("루틴에 없는 productId: " + ps.getId());
            }
            item.applyUserSelection(ps.getSlot(), ps.getAction());
        }
        routineItemRepository.saveAll(items);

        // 유지(KEEP)로 선택된 슬롯 레코드 수 -> afterCount (BOTH 양쪽 유지 시 자동 2)
        long afterCount = routineItemSelectionRepository.countByRoutineIdAndAction(routineId, RecommendStatus.KEEP);
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
