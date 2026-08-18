package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityIssueDto;
import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityDuplicateReportDto;
import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityIngredientNoticeDto;
import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityIssueDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.exception.ProductNotFoundException;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.entity.Allergic;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.ingredient.data.repository.AllergicRepository;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import com.nyyb.nyybserver.routine.data.exception.RoutineNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityService {

    private static final String REPORT_TITLE = "분석이 모두 완료되었어요!";
    private static final String REPORT_DESCRIPTION =
            "구매 예정인 제품과 기존 제품의 성분을 비교했어요.";
    private static final String INGREDIENT_NOTICE_TITLE =
            "주의 정보가 등록된 성분이 포함되어 있어요.";
    private static final String MFDS_SOURCE = "식품의약품안전처";
    private static final String ALLERGEN_NOTICE =
            "해당 성분은 알레르기 유발 가능성이 있습니다.";
    private static final String ALLERGEN_THRESHOLD_NOTICE =
            "※ 다만, 사용 후 씻어내는 제품에는 0.01% 초과, 사용 후 씻어내지 않는 제품에는 0.001% 초과 함유하는 경우에 한한다.";
    private static final String PROHIBITED_INGREDIENT_NOTICE =
            "사용금지된 성분입니다.";
    private static final String INGREDIENT_INFORMATION_DISCLAIMER =
            "본 정보는 식품의약품안전처 공개 기준을 그대로 안내하는 것이며, 개별 사용자의 사용 적합성·안전성 또는 알레르기 발생 여부를 진단·보증하는 것이 아닙니다.";
    private static final Pattern USAGE_LIMIT_PATTERN = Pattern.compile(
            "사용기준\\s*[:：]\\s*(\\d+(?:\\.\\d+)?%)(?=\\s|[.,。]|$)"
    );
    private static final String DISCLAIMER =
            "본 분석은 성분 정보 참고용이며, 효능·안전성을 진단·보증하지 않습니다.";
    private static final String UNKNOWN_SUMMARY =
            "인식된 성분이나 현재 루틴 정보가 부족해 정확한 비교가 어려워요.";
    private static final String UNKNOWN_GUIDE =
            "성분표가 선명한 사진으로 다시 촬영하거나 현재 루틴을 먼저 저장해 주세요.";
    private static final String DEFAULT_ISSUE_REASON =
            "성분 또는 제품 역할 구성이 겹쳐 사용 시간대를 나누는 방법을 고려해볼 수 있어요.";
    private static final List<String> UNSAFE_WORDS = List.of(
            "안전", "위험", "독성", "유해", "자극", "알레르", "부작용", "치료", "처방", "효능",
            "효과", "개선", "강화", "예방", "완화", "염증", "질환", "트러블", "피부에 맞", "반드시",
            "절대", "무조건"
    );

    private final OcrService ocrService;
    private final CompatibilityAnalyzer compatibilityAnalyzer;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final AllergicRepository allergicRepository;
    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;

    public CompatibilityResponseDto compare(MultipartFile image, Long userId) {
        Routine routine = routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .orElseThrow(RoutineNotFoundException::new);

        OcrResponseDto ocrResult = ocrService.ocr(image, userId);
        Product candidate = productRepository.findByIdAndUserId(ocrResult.getProductId(), userId)
                .orElseThrow(ProductNotFoundException::new);

        List<ProductIngredient> candidateIngredients =
                productIngredientRepository.findByProductIdWithIngredient(candidate.getId());
        List<RoutineItem> routineItems =
                routineItemRepository.findByRoutineIdWithProductAndSelections(routine.getId());
        List<RoutineProductContext> currentProducts = currentProducts(routineItems);

        if (currentProducts.isEmpty() || !hasCandidateData(candidate, candidateIngredients)) {
            return unknownResponse(candidate, ocrResult, routine, candidateIngredients);
        }

        log.info("신규 제품 궁합 분석을 요청합니다. userId={}, routineId={}, productId={}",
                userId, routine.getId(), candidate.getId());

        LlmCompatibilityResponseDto analysis = compatibilityAnalyzer.analyze(
                buildUserMessage(candidate, candidateIngredients, routine, currentProducts)
        );

        return toResponse(candidate, ocrResult, routine, candidateIngredients, currentProducts, analysis);
    }

    private List<RoutineProductContext> currentProducts(List<RoutineItem> items) {
        boolean hasSavedSelections = items.stream()
                .anyMatch(item -> !item.getSelections().isEmpty());
        List<RoutineProductContext> products = new ArrayList<>();

        for (RoutineItem item : items) {
            List<RoutineSlot> activeSlots = List.of(RoutineSlot.MORNING, RoutineSlot.EVENING).stream()
                    .filter(slot -> isActive(item, slot, hasSavedSelections))
                    .toList();

            if (!activeSlots.isEmpty()) {
                products.add(new RoutineProductContext(
                        item.getProduct(),
                        activeSlots,
                        productIngredientRepository.findByProductIdWithIngredient(item.getProduct().getId())
                ));
            }
        }
        return products;
    }

    private boolean isActive(RoutineItem item, RoutineSlot slot, boolean hasSavedSelections) {
        if (!matchesSlot(item.getUserRoutineSlot(), slot)) {
            return false;
        }

        if (hasSavedSelections) {
            return item.getSelections().stream()
                    .filter(selection -> selection.getSlot() == slot)
                    .anyMatch(selection -> selection.getAction() == RecommendStatus.KEEP);
        }

        return item.getRecommended() != RecommendStatus.REMOVE
                || !matchesSlot(item.getLlmRoutineSlot(), slot);
    }

    private boolean matchesSlot(RoutineSlot itemSlot, RoutineSlot targetSlot) {
        return itemSlot == targetSlot || itemSlot == RoutineSlot.BOTH;
    }

    private boolean hasCandidateData(Product candidate, List<ProductIngredient> ingredients) {
        return StringUtils.hasText(candidate.getOcrText()) || !ingredients.isEmpty();
    }

    private CompatibilityResponseDto unknownResponse(
            Product candidate,
            OcrResponseDto ocrResult,
            Routine routine,
            List<ProductIngredient> candidateIngredients
    ) {
        return new CompatibilityResponseDto(
                candidate.getId(),
                "새 제품",
                candidate.getCategory(),
                ocrResult.getIngredientCount(),
                safeList(ocrResult.getIngredients()),
                routine.getId(),
                CompatibilityStatus.UNKNOWN,
                0,
                RoutineSlot.BOTH,
                UNKNOWN_SUMMARY,
                UNKNOWN_GUIDE,
                List.of(),
                REPORT_TITLE,
                REPORT_DESCRIPTION,
                purchaseAdvice(CompatibilityStatus.UNKNOWN),
                null,
                INGREDIENT_NOTICE_TITLE,
                buildIngredientNotices(candidateIngredients),
                DISCLAIMER
        );
    }

    private CompatibilityResponseDto toResponse(
            Product candidate,
            OcrResponseDto ocrResult,
            Routine routine,
            List<ProductIngredient> candidateIngredients,
            List<RoutineProductContext> currentProducts,
            LlmCompatibilityResponseDto analysis
    ) {
        CompatibilityStatus status = analysis.status() == null
                ? CompatibilityStatus.UNKNOWN
                : analysis.status();
        List<CompatibilityIssueDto> issues =
                sanitizeIssues(analysis.issues(), candidateIngredients, currentProducts);

        return new CompatibilityResponseDto(
                candidate.getId(),
                StringUtils.hasText(analysis.productName()) ? analysis.productName().strip() : "새 제품",
                candidate.getCategory(),
                ocrResult.getIngredientCount(),
                safeList(ocrResult.getIngredients()),
                routine.getId(),
                status,
                clampScore(analysis.score()),
                analysis.recommendedSlot() == null ? RoutineSlot.BOTH : analysis.recommendedSlot(),
                safeText(analysis.summary(), defaultSummary(status)),
                safeText(analysis.usageGuide(), defaultUsageGuide(status)),
                issues,
                REPORT_TITLE,
                REPORT_DESCRIPTION,
                purchaseAdvice(status),
                buildDuplicateReport(issues),
                INGREDIENT_NOTICE_TITLE,
                buildIngredientNotices(candidateIngredients),
                DISCLAIMER
        );
    }

    private List<CompatibilityIssueDto> sanitizeIssues(
            List<LlmCompatibilityIssueDto> issues,
            List<ProductIngredient> candidateIngredients,
            List<RoutineProductContext> currentProducts
    ) {
        if (issues == null) {
            return List.of();
        }

        Map<Long, RoutineProductContext> contextByProductId = currentProducts.stream()
                .collect(Collectors.toMap(context -> context.product().getId(), Function.identity()));

        return issues.stream()
                .filter(issue -> issue != null && issue.routineProductId() != null)
                .filter(issue -> contextByProductId.containsKey(issue.routineProductId()))
                .map(issue -> {
                    RoutineProductContext context = contextByProductId.get(issue.routineProductId());
                    List<String> overlappingIngredients =
                            overlappingIngredients(candidateIngredients, context.ingredients());
                    return new CompatibilityIssueDto(
                            normalizeIssueSlot(issue.slot(), context.slots()),
                            context.product().getId(),
                            displayName(context.product()),
                            overlappingIngredients.size(),
                            overlappingIngredients,
                            safeText(issue.reason(), DEFAULT_ISSUE_REASON)
                    );
                })
                .toList();
    }

    /** 화면 상단의 구매 안내 문구를 궁합 상태에 따라 결정한다. */
    private String purchaseAdvice(CompatibilityStatus status) {
        return switch (status) {
            case GOOD -> "구매해도 괜찮아요.";
            case CAUTION -> "구매 전 중복 성분을 한 번 더 확인해볼 수 있어요.";
            case NOT_RECOMMENDED -> "구매하지 않아도 괜찮아요.";
            case UNKNOWN -> "성분 정보를 다시 확인해 주세요.";
        };
    }

    /** 실제로 같은 성분이 확인된 제품만 중복 카드에 포함한다. */
    private CompatibilityDuplicateReportDto buildDuplicateReport(List<CompatibilityIssueDto> issues) {
        List<CompatibilityIssueDto> duplicatedProducts = issues.stream()
                .filter(issue -> issue.overlappingIngredientCount() > 0)
                .toList();

        if (duplicatedProducts.isEmpty()) {
            return null;
        }

        return new CompatibilityDuplicateReportDto(
                duplicatedProducts.size(),
                "루틴에서 사용 중인 제품들과 성분이 중복되어 있어요.",
                duplicatedProducts
        );
    }

    /**
     * 알레르기 등록 성분이나 DB 설명에서 사용금지·제한 근거가 명확한 성분만 전달한다.
     * 위험도나 독성 값만 보고 별도의 위험 문구를 추측해서 만들지 않는다.
     */
    private List<CompatibilityIngredientNoticeDto> buildIngredientNotices(
            List<ProductIngredient> productIngredients
    ) {
        Map<String, Allergic> allergicIndex = allergicRepository.findAll().stream()
                .collect(Collectors.toMap(
                        allergic -> normalizeName(allergic.getName()),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        Map<String, CompatibilityIngredientNoticeDto> notices = new LinkedHashMap<>();

        for (ProductIngredient productIngredient : productIngredients) {
            Ingredient ingredient = productIngredient.getIngredient();
            if (ingredient == null || !StringUtils.hasText(ingredient.getName())) {
                continue;
            }

            Allergic allergic = allergicIndex.get(normalizeName(ingredient.getName()));
            String regulatoryNotice = regulatoryNotice(ingredient);
            if (allergic == null && regulatoryNotice == null) {
                continue;
            }

            String key = ingredient.getId() == null
                    ? normalizeName(ingredient.getName())
                    : String.valueOf(ingredient.getId());
            notices.putIfAbsent(key, new CompatibilityIngredientNoticeDto(
                    ingredient.getId(),
                    ingredient.getName(),
                    ingredient.getRiskLevel(),
                    allergic != null,
                    MFDS_SOURCE,
                    allergic == null ? null : ALLERGEN_NOTICE,
                    allergic == null ? null : ALLERGEN_THRESHOLD_NOTICE,
                    regulatoryNotice,
                    ingredient.getDescription(),
                    INGREDIENT_INFORMATION_DISCLAIMER
            ));
        }

        return List.copyOf(notices.values());
    }

    /**
     * DB 원문에 제한 근거와 사용기준 수치가 함께 있을 때만 제한 문구를 만든다.
     * 수치는 정규식으로 원문 그대로 가져오며 반올림하거나 임의 보정하지 않는다.
     */
    private String regulatoryNotice(Ingredient ingredient) {
        if (ingredient.getRiskLevel() != RiskLevel.DISALLOWED
                || !StringUtils.hasText(ingredient.getDescription())) {
            return null;
        }

        String description = ingredient.getDescription();
        Matcher usageLimitMatcher = USAGE_LIMIT_PATTERN.matcher(description);
        if (description.contains("제한 원료") && usageLimitMatcher.find()) {
            return "사용기준 " + usageLimitMatcher.group(1) + "로 제한된 성분입니다.";
        }
        if (description.contains("사용금지 원료")) {
            return PROHIBITED_INGREDIENT_NOTICE;
        }
        return null;
    }

    private RoutineSlot normalizeIssueSlot(RoutineSlot requested, List<RoutineSlot> activeSlots) {
        if (requested != null
                && (requested == RoutineSlot.BOTH || activeSlots.contains(requested))) {
            return requested;
        }
        return activeSlots.size() == 2 ? RoutineSlot.BOTH : activeSlots.getFirst();
    }

    private List<String> overlappingIngredients(
            List<ProductIngredient> candidateIngredients,
            List<ProductIngredient> routineIngredients
    ) {
        Set<String> routineNames = routineIngredients.stream()
                .map(this::ingredientName)
                .filter(StringUtils::hasText)
                .map(this::normalizeName)
                .collect(Collectors.toSet());

        Set<String> overlapping = new LinkedHashSet<>();
        for (ProductIngredient candidateIngredient : candidateIngredients) {
            String name = ingredientName(candidateIngredient);
            if (StringUtils.hasText(name) && routineNames.contains(normalizeName(name))) {
                overlapping.add(name);
            }
        }
        return List.copyOf(overlapping);
    }

    private String buildUserMessage(
            Product candidate,
            List<ProductIngredient> candidateIngredients,
            Routine routine,
            List<RoutineProductContext> currentProducts
    ) {
        StringBuilder message = new StringBuilder();
        message.append("=== 새 제품 ===\n")
                .append("productId: ").append(candidate.getId()).append('\n')
                .append("category: ").append(candidate.getCategory()).append('\n')
                .append("ocrText:\n<ocr-data>\n")
                .append(candidate.getOcrText() == null ? "" : candidate.getOcrText())
                .append("\n</ocr-data>\n")
                .append("matchedIngredients:")
                .append(formatIngredients(candidateIngredients))
                .append("\n\n=== 현재 루틴 ===\n")
                .append("routineId: ").append(routine.getId()).append("\n\n");

        for (RoutineProductContext context : currentProducts) {
            Product product = context.product();
            message.append("--- 기존 루틴 제품 ---\n")
                    .append("productId: ").append(product.getId()).append('\n')
                    .append("productName: ").append(displayName(product)).append('\n')
                    .append("category: ").append(product.getCategory()).append('\n')
                    .append("activeSlots: ").append(formatSlots(context.slots())).append('\n')
                    .append("ingredients:").append(formatIngredients(context.ingredients()))
                    .append("\n\n");
        }
        return message.toString();
    }

    private String formatSlots(List<RoutineSlot> slots) {
        return slots.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private String formatIngredients(List<ProductIngredient> productIngredients) {
        if (productIngredients.isEmpty()) {
            return " (인식된 성분 없음)";
        }
        return productIngredients.stream()
                .map(this::formatIngredient)
                .collect(Collectors.joining("\n  - ", "\n  - ", ""));
    }

    private String formatIngredient(ProductIngredient productIngredient) {
        Ingredient ingredient = productIngredient.getIngredient();
        if (ingredient == null) {
            return productIngredient.getRawName();
        }

        String riskLevel = ingredient.getRiskLevel() == null
                ? "UNKNOWN"
                : ingredient.getRiskLevel().name();
        String description = StringUtils.hasText(ingredient.getDescription())
                ? ", description=" + ingredient.getDescription()
                : "";
        return ingredient.getName()
                + " (riskLevel=" + riskLevel
                + ", toxic=" + Boolean.TRUE.equals(ingredient.getIsToxic())
                + description + ")";
    }

    private String ingredientName(ProductIngredient productIngredient) {
        Ingredient ingredient = productIngredient.getIngredient();
        return ingredient == null ? productIngredient.getRawName() : ingredient.getName();
    }

    private String normalizeName(String value) {
        return value.replaceAll("[\\s_-]", "").toLowerCase(Locale.ROOT);
    }

    private String displayName(Product product) {
        return StringUtils.hasText(product.getProductName())
                ? product.getProductName()
                : product.getCategory().name() + " " + product.getId();
    }

    private int clampScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String safeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return UNSAFE_WORDS.stream().anyMatch(normalized::contains)
                ? fallback
                : value.strip();
    }

    private String defaultSummary(CompatibilityStatus status) {
        return switch (status) {
            case GOOD -> "현재 루틴과 성분 및 역할 구성이 크게 겹치지 않아 추가를 고려해볼 수 있어요.";
            case CAUTION -> "현재 루틴과 겹치는 구성이 있어 사용 시간대를 나누는 방법을 고려해볼 수 있어요.";
            case NOT_RECOMMENDED -> "현재 루틴과 성분 및 역할 구성이 많이 겹쳐 추가 여부를 다시 살펴볼 수 있어요.";
            case UNKNOWN -> UNKNOWN_SUMMARY;
        };
    }

    private String defaultUsageGuide(CompatibilityStatus status) {
        return switch (status) {
            case GOOD -> "추천 시간대의 기존 루틴에 한 단계씩 추가하는 방법을 고려해볼 수 있어요.";
            case CAUTION -> "겹치는 제품과 서로 다른 시간대에 사용하는 방법을 고려해볼 수 있어요.";
            case NOT_RECOMMENDED -> "같은 역할의 기존 제품과 번갈아 사용하는 방법을 고려해볼 수 있어요.";
            case UNKNOWN -> UNKNOWN_GUIDE;
        };
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record RoutineProductContext(
            Product product,
            List<RoutineSlot> slots,
            List<ProductIngredient> ingredients
    ) {
    }
}
