package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.request.AnalysisRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisSummaryDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmAnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmProductAnalysisDto;
import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.exception.AnalysisNotFoundException;
import com.nyyb.nyybserver.analysis.data.exception.ProductNotFoundException;
import com.nyyb.nyybserver.analysis.data.repository.AnalysisRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import com.nyyb.nyybserver.user.data.entity.User;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    // 응답 정렬: REMOVE 먼저, KEEP 나중 (analyze·상세 조회 공용)
    private static final Comparator<LlmProductAnalysisDto> REMOVE_FIRST =
            Comparator.comparingInt(p -> p.recommended() == RecommendStatus.REMOVE ? 0 : 1);

    private final ChatClient chatClient;
    private final AnalysisRepository analysisRepository;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;
    private final UserRepository userRepository;

    /**
     * 제품들 -> LLM 제외/유지 분석 -> Product·Analysis 반영 -> Routine·RoutineItem 생성(saveRoutine 통합) -> routineId + LLM 응답 반환
     * @param request productId + userRoutineSlot 목록
     * @param userId  소유자로 지정할 현재 로그인 유저 id(게스트/카카오 공통)
     * @return AnalysisResponseDto (routineId + 제품별 분석 결과)
     */
    @Transactional
    public AnalysisResponseDto analyze(AnalysisRequestDto request, Long userId) {
        List<AnalysisRequestDto.ProductSlot> productSlots = request.getProducts();
        List<Long> productIds = productSlots.stream()
                .map(AnalysisRequestDto.ProductSlot::getProductId)
                .toList();

        // 현재 로그인 사용자(게스트/카카오 공통)를 소유자로 지정
        User owner = userRepository.getReferenceById(userId);

        // 요청한 제품이 모두 본인 소유인지 검증. 하나라도 남의 것이면 전체 거절한다.
        // (Product.id가 순차 증가라 열거가 쉬우므로, 검증 없이는 남의 제품을 자기 분석에 재매핑할 수 있다)
        Map<Long, Product> productMap = loadOwnedProducts(productIds, userId);

        String userMessage = buildUserMessage(productIds, productMap);
        log.info("OpenAI 요청 메시지:\n{}", userMessage);

        // LLM 호출 + 구조화 출력(JSON -> DTO)
        LlmAnalysisResponseDto llmResponse = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(LlmAnalysisResponseDto.class);

        log.info("OpenAI 응답:\n{}", llmResponse);

        // 한국 날짜 + 제품 개수로 만든 목록 표시용 문구 (분석·루틴 공용)
        String title = buildTitle(productIds.size());

        // Analysis 저장 후 결과를 각 Product에 반영(더티 체킹)
        Analysis analysis = analysisRepository.save(Analysis.builder()
                .user(owner)
                .title(title)
                .build());
        llmResponse.products().forEach(result -> applyToProduct(analysis, result, productMap));

        // saveRoutine 통합: 분석 1개당 Routine 1개 생성 + productId별 userRoutineSlot을 RoutineItem으로 저장
        Routine routine = routineRepository.save(Routine.builder()
                .user(owner)
                .analysis(analysis)
                .title(title)
                .beforeCount(productIds.size())
                .build());

        for (AnalysisRequestDto.ProductSlot productSlot : productSlots) {
            routineItemRepository.save(RoutineItem.builder()
                    .routine(routine)
                    .product(productMap.get(productSlot.getProductId()))
                    .userRoutineSlot(productSlot.getUserRoutineSlot())
                    .build());
        }

        // REMOVE 먼저, KEEP 나중 순으로 정렬해 반환
        List<LlmProductAnalysisDto> sorted = llmResponse.products().stream()
                .sorted(REMOVE_FIRST)
                .toList();
        return new AnalysisResponseDto(routine.getId(), title, sorted);
    }

    /**
     * 현재 로그인 유저(게스트/카카오 공통)의 분석 목록을 최신순으로 페이징 조회
     * @param userId   소유자 id
     * @param pageable page/size 페이징 정보
     * @return id + title 목록
     */
    @Transactional(readOnly = true)
    public List<AnalysisSummaryDto> getAnalyses(Long userId, Pageable pageable) {
        return analysisRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable).stream()
                .map(AnalysisSummaryDto::from)
                .toList();
    }

    /**
     * 분석 상세 조회. 저장된 Analysis·Product·Routine으로 analyze와 동일한 응답을 재구성한다.
     * @param analysisId 조회할 분석 id
     * @param userId     소유자 id (본인 분석만 조회 가능)
     * @return AnalysisResponseDto (routineId + title + 제품별 분석 결과)
     * @throws AnalysisNotFoundException 해당 id의 분석이 없거나 본인 소유가 아닌 경우
     */
    @Transactional(readOnly = true)
    public AnalysisResponseDto getAnalysis(UUID analysisId, Long userId) {
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, userId)
                .orElseThrow(AnalysisNotFoundException::new);

        // analyze 단계에서 분석 1개당 1개 생성된 루틴 id (designRoutine 호출용)
        UUID routineId = routineRepository.findFirstByAnalysisIdOrderByCreatedAtAsc(analysisId)
                .map(Routine::getId)
                .orElse(null);

        // REMOVE 먼저, KEEP 나중 순으로 정렬해 반환 (analyze와 동일)
        List<LlmProductAnalysisDto> products = productRepository.findByAnalysisIdOrderByIdAsc(analysisId).stream()
                .map(product -> new LlmProductAnalysisDto(
                        product.getId(),
                        product.getProductName(),
                        product.getRecommended(),
                        product.getRecommendReason()))
                .sorted(REMOVE_FIRST)
                .toList();

        return new AnalysisResponseDto(routineId, analysis.getTitle(), products);
    }

    // 한국 기준 오늘 날짜 + 제품 개수 -> "8월 3일 5개의 제품"
    private String buildTitle(int productCount) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        return today.getMonthValue() + "월 " + today.getDayOfMonth() + "일 " + productCount + "개의 제품";
    }

    /**
     * 요청한 productId를 본인 소유로 한정해 조회한다.
     * 하나라도 없거나 남의 제품이면 전체를 거절한다(어떤 id가 남의 것인지 알려주지 않기 위해 개별 구분 없이 처리).
     * @param productIds 요청에 담긴 제품 id 목록 (중복 가능)
     * @param userId     현재 로그인 유저 id
     * @return productId -> Product 매핑
     * @throws ProductNotFoundException 요청 id 중 본인 소유가 아닌 것이 있는 경우
     */
    private Map<Long, Product> loadOwnedProducts(List<Long> productIds, Long userId) {
        List<Long> distinctIds = productIds.stream().distinct().toList();

        List<Product> products = productRepository.findByIdInAndUserId(distinctIds, userId);
        if (products.size() != distinctIds.size()) {
            throw new ProductNotFoundException();
        }

        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    // 제품별 productId + category + ocrText + 성분 -> 프롬프트 텍스트로 조립
    private String buildUserMessage(List<Long> productIds, Map<Long, Product> productMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 제품들을 분석해 주세요.\n\n");

        for (Long productId : productIds) {
            Product product = productMap.get(productId);

            sb.append("=== productId: ").append(productId).append(" ===\n");
            sb.append("category: ").append(product.getCategory().name()).append("\n");
            sb.append("ocrText: ").append(product.getOcrText()).append("\n");
            sb.append("ingredients: ").append(formatIngredients(productId)).append("\n\n");
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

    // 결과 -> 같은 productId Product에 반영. LLM이 요청에 없던 id를 지어내면 거절한다.
    private void applyToProduct(Analysis analysis, LlmProductAnalysisDto result, Map<Long, Product> productMap) {
        Product product = productMap.get(result.productId());
        if (product == null) {
            throw new IllegalArgumentException("요청에 없는 productId: " + result.productId());
        }

        product.applyAnalysis(analysis, result.productName(), result.recommended(), result.recommendReason());
    }
}
