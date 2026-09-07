package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.request.AnalysisRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisProductDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisSummaryDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmAnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmProductAnalysisDto;
import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import com.nyyb.nyybserver.product.data.dto.response.ProductIngredientMappingDto;
import com.nyyb.nyybserver.product.data.entity.Product;
import com.nyyb.nyybserver.product.data.entity.UserProduct;
import com.nyyb.nyybserver.product.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.exception.AnalysisNotFoundException;
import com.nyyb.nyybserver.product.data.exception.ProductNotFoundException;
import com.nyyb.nyybserver.analysis.data.repository.AnalysisRepository;
import com.nyyb.nyybserver.product.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.product.data.repository.ProductRepository;
import com.nyyb.nyybserver.product.data.repository.UserProductRepository;
import com.nyyb.nyybserver.product.service.ProductService;
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
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
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

    // 이유 문구가 비어 온 경우에만 쓰는 기본값. 화면에 null·빈 칸이 그대로 나가지 않게 한다.
    private static final String DEFAULT_KEEP_REASON = "겹치는 성분이 적어 유지를 고려해볼 수 있어요.";
    private static final String DEFAULT_REMOVE_REASON = "성분 구성이 겹쳐 제외를 고려해볼 수 있어요.";

    // 응답 정렬: REMOVE 먼저, KEEP 나중 (analyze·상세 조회 공용)
    private static final Comparator<AnalysisProductDto> REMOVE_FIRST =
            Comparator.comparingInt(p -> p.recommended() == RecommendStatus.REMOVE ? 0 : 1);

    private final ChatClient chatClient;
    private final AnalysisRepository analysisRepository;
    private final ProductRepository productRepository;
    private final UserProductRepository userProductRepository;
    private final ProductService productService;
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

        // 요청 productId는 제품 마스터(카탈로그) id다. 존재하지 않는 id가 섞이면 전체 거절한다.
        Map<Long, Product> productMap = loadProducts(productIds);

        // 프롬프트에 넣는 제품 순서는 productId 오름차순으로 고정한다.
        List<Long> orderedProductIds = productIds.stream().distinct().sorted().toList();

        // 프롬프트에 실을 성분을 먼저 채운다. 마스터에 전성분이 아직 없는 제품만 웹 조회를 타고,
        // 이미 매핑돼 있으면 ProductService가 LLM 호출 없이 기존 매핑을 그대로 쓴다.
        orderedProductIds.forEach(this::ensureIngredientsMapped);

        String userMessage = buildUserMessage(orderedProductIds, productMap);
        log.info("OpenAI 요청 메시지:\n{}", userMessage);

        // LLM 호출 + 구조화 출력(JSON -> DTO)
        LlmAnalysisResponseDto llmResponse = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(LlmAnalysisResponseDto.class);

        log.info("OpenAI 응답:\n{}", llmResponse);

        // strict 스키마라 필드 누락·null은 모델이 만들 수 없지만, 호출 자체가 빈 응답으로 끝나는 경우는 막아 둔다.
        if (llmResponse == null || llmResponse.products() == null) {
            throw new IllegalStateException("분석 응답이 비어 있습니다.");
        }

        // 한국 날짜 + 제품 개수로 만든 목록 표시용 문구 (분석·루틴 공용)
        String title = buildTitle(productIds.size());

        Analysis analysis = analysisRepository.save(Analysis.builder()
                .user(owner)
                .title(title)
                .build());

        // 분석 시점에 사용자 제품(UserProduct)을 생성한다. 마스터 productId -> UserProduct 매핑으로
        // LLM 응답(마스터 id 기준)을 사용자 제품에 되돌려 붙인다.
        Map<Long, UserProduct> userProducts = new HashMap<>();
        for (Long productId : orderedProductIds) {
            userProducts.put(productId, userProductRepository.save(UserProduct.builder()
                    .user(owner)
                    .product(productMap.get(productId))
                    .analysis(analysis)
                    .build()));
        }

        // 저장 값과 응답 값이 갈라지지 않도록 제품별 결과를 한 번만 만들어 두 곳에 함께 쓴다.
        List<AnalysisProductDto> analyzed = llmResponse.products().stream()
                .map(result -> toProductDto(result, productMap, userProducts))
                .toList();

        llmResponse.products().forEach(result -> userProducts.get(result.productId())
                .applyAnalysis(analysis, result.recommended(), resolveReason(result)));

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
                    .userProduct(userProducts.get(productSlot.getProductId()))
                    .userRoutineSlot(productSlot.getUserRoutineSlot())
                    .build());
        }

        // REMOVE 먼저, KEEP 나중 순으로 정렬해 반환
        List<AnalysisProductDto> sorted = analyzed.stream()
                .sorted(REMOVE_FIRST)
                .toList();
        return new AnalysisResponseDto(routine.getId(), title, sorted);
    }

    /**
     * 현재 로그인 유저(게스트/카카오 공통)의 분석 목록을 최신순으로 페이징 조회
     * @param userId   소유자 id
     * @param pageable page/size 페이징 정보
     * @return id + title + 제품 수 + REMOVE 제안 수 목록
     */
    @Transactional(readOnly = true)
    public List<AnalysisSummaryDto> getAnalyses(Long userId, Pageable pageable) {
        List<Analysis> analyses = analysisRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        Map<UUID, List<UserProductRepository.RecommendCount>> countsByAnalysisId = summarizeProductCounts(analyses);

        return analyses.stream()
                .map(analysis -> {
                    List<UserProductRepository.RecommendCount> counts =
                            countsByAnalysisId.getOrDefault(analysis.getId(), List.of());
                    long productCount = counts.stream()
                            .mapToLong(UserProductRepository.RecommendCount::getCount)
                            .sum();
                    long removeCount = counts.stream()
                            .filter(count -> count.getRecommended() == RecommendStatus.REMOVE)
                            .mapToLong(UserProductRepository.RecommendCount::getCount)
                            .sum();
                    return AnalysisSummaryDto.from(analysis, productCount, removeCount);
                })
                .toList();
    }

    // 분석 목록의 productId를 한 번에 집계 조회해 analysisId별로 그룹핑 (N+1 방지)
    private Map<UUID, List<UserProductRepository.RecommendCount>> summarizeProductCounts(List<Analysis> analyses) {
        if (analyses.isEmpty()) {
            return Map.of();
        }

        List<UUID> analysisIds = analyses.stream().map(Analysis::getId).toList();
        return userProductRepository.countGroupByAnalysisIdAndRecommended(analysisIds).stream()
                .collect(Collectors.groupingBy(UserProductRepository.RecommendCount::getAnalysisId));
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
        List<AnalysisProductDto> products = userProductRepository.findByAnalysisIdWithProduct(analysisId).stream()
                .map(userProduct -> new AnalysisProductDto(
                        userProduct.getId(),
                        displayName(userProduct.getProduct()),
                        userProduct.getRecommended(),
                        userProduct.getRecommendReason()))
                .sorted(REMOVE_FIRST)
                .toList();

        return new AnalysisResponseDto(routineId, analysis.getTitle(), products);
    }

    /**
     * 분석 삭제. 실제 행을 지우지 않고 소유자만 해제해 유저의 목록·상세에서 사라지게 한다.
     * (제품·루틴 등 참조 데이터를 그대로 살려두기 위해 소프트 삭제로 처리)
     * @param analysisId 삭제할 분석 id
     * @param userId     소유자 id (본인 분석만 삭제 가능)
     * @throws AnalysisNotFoundException 해당 id의 분석이 없거나 본인 소유가 아닌 경우
     */
    @Transactional
    public void deleteAnalysis(UUID analysisId, Long userId) {
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, userId)
                .orElseThrow(AnalysisNotFoundException::new);

        analysis.releaseOwner();
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
    private Map<Long, Product> loadProducts(List<Long> productIds) {
        List<Long> distinctIds = productIds.stream().distinct().toList();

        List<Product> products = productRepository.findAllById(distinctIds);
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
            sb.append("productName: ").append(product.getItemName()).append("\n");
            sb.append("categoryMain: ").append(product.getCategoryMainLabel()).append("\n");
            sb.append("categorySub: ").append(product.getCategorySubLabel()).append("\n");
            sb.append("ingredients: ").append(formatIngredients(productId)).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 제품 마스터에 전성분이 없으면 웹에서 찾아 성분 마스터와 매핑해 둔다.
     * 조회에 실패해도 분석 자체는 진행한다. (성분이 비면 프롬프트에 "인식된 성분 없음"으로 나간다)
     */
    private void ensureIngredientsMapped(Long productId) {
        try {
            ProductIngredientMappingDto mapping = productService.mapIngredientsFromWeb(productId);
            if (!mapping.alreadyMapped()) {
                log.info("전성분을 새로 매핑했습니다. productId={}, 매칭 {}/{}건",
                        productId, mapping.matchedCount(), mapping.totalCount());
            }
        } catch (RuntimeException e) {
            log.warn("전성분 조회에 실패해 기존 성분만으로 분석합니다. productId={}", productId, e);
        }
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

    /**
     * LLM 제품별 결과 -> 응답 DTO. LLM이 요청에 없던 마스터 productId를 지어내면 거절한다.
     * 응답에 싣는 식별자는 마스터 id가 아니라 사용자 제품(UserProduct)의 id다.
     * @param result       LLM 판단 결과 (마스터 productId 기준)
     * @param productMap   마스터 productId -> Product
     * @param userProducts 마스터 productId -> UserProduct
     * @throws IllegalArgumentException 요청에 없던 productId인 경우
     */
    private AnalysisProductDto toProductDto(LlmProductAnalysisDto result,
                                            Map<Long, Product> productMap, Map<Long, UserProduct> userProducts) {
        if (!productMap.containsKey(result.productId())) {
            throw new IllegalArgumentException("요청에 없는 productId: " + result.productId());
        }

        return new AnalysisProductDto(
                userProducts.get(result.productId()).getId(),
                displayName(productMap.get(result.productId())),
                result.recommended(),
                resolveReason(result));
    }

    // 이유 문구가 비어 오면 판단에 맞는 기본 문구로 채운다. (null·빈 문자열이 화면에 그대로 나가지 않게)
    private String resolveReason(LlmProductAnalysisDto result) {
        if (StringUtils.hasText(result.recommendReason())) {
            return result.recommendReason().strip();
        }
        return result.recommended() == RecommendStatus.REMOVE ? DEFAULT_REMOVE_REASON : DEFAULT_KEEP_REASON;
    }

    // 품목명이 비어 있는 마스터 행 방어. (전체 19.5만 건 중 품목명 없는 행이 존재한다)
    private String displayName(Product product) {
        return StringUtils.hasText(product.getItemName()) ? product.getItemName() : "이름 미상 제품";
    }
}
