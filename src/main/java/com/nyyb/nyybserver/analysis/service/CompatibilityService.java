package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.request.CompatibilityRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.exception.ProductNotFoundException;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.dto.response.ProductIngredientMatchDto;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.service.IngredientService;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import com.nyyb.nyybserver.routine.data.exception.RoutineNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityService {

    private static final String UNKNOWN_SUMMARY =
            "인식된 성분이나 현재 루틴 정보가 부족해 정확한 비교가 어려워요.";
    private static final String UNKNOWN_GUIDE =
            "성분표가 선명한 사진으로 다시 촬영하거나 현재 루틴을 먼저 저장해 주세요.";
    private static final List<String> UNSAFE_WORDS = List.of(
            "안전", "위험", "독성", "유해", "자극", "알레르", "부작용", "치료", "처방", "효능",
            "효과", "개선", "강화", "예방", "완화", "염증", "질환", "트러블", "피부에 맞", "반드시",
            "절대", "무조건"
    );

    private final CompatibilityAnalyzer compatibilityAnalyzer;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;
    private final IngredientService ingredientService;

    public CompatibilityResponseDto compare(CompatibilityRequestDto request, Long userId) {
        Routine routine = routineRepository.findByIdAndUserId(request.getRoutineId(), userId)
                .orElseThrow(RoutineNotFoundException::new);
        Product candidate = productRepository.findByIdAndUserId(request.getProductId(), userId)
                .orElseThrow(ProductNotFoundException::new);

        List<ProductIngredient> candidateIngredients =
                productIngredientRepository.findByProductIdWithIngredient(candidate.getId());
        List<RoutineItem> routineItems =
                routineItemRepository.findByRoutineIdWithProductAndSelections(routine.getId());
        List<RoutineProductContext> currentProducts = currentProducts(routineItems);

        // 알레르기·유해성분 매칭은 ingredient match API와 동일한 로직 사용 (요청받은 productId 기준)
        ProductIngredientMatchDto ingredientMatch = ingredientService.match(candidate.getId());

        if (currentProducts.isEmpty() || !hasCandidateData(candidate, candidateIngredients)) {
            return unknownResponse(candidate, ingredientMatch);
        }

        log.info("신규 제품 궁합 분석을 요청합니다. userId={}, routineId={}, productId={}",
                userId, routine.getId(), candidate.getId());

        LlmCompatibilityResponseDto analysis = compatibilityAnalyzer.analyze(
                buildUserMessage(candidate, candidateIngredients, routine, currentProducts)
        );

        return toResponse(candidate, candidateIngredients, currentProducts, analysis, ingredientMatch);
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
            ProductIngredientMatchDto ingredientMatch
    ) {
        return new CompatibilityResponseDto(
                candidate.getId(),
                "새 제품",
                RecommendStatus.KEEP,
                UNKNOWN_SUMMARY + " " + UNKNOWN_GUIDE,
                ingredientMatch.ingredients(),
                ingredientMatch.allergics()
        );
    }

    private CompatibilityResponseDto toResponse(
            Product candidate,
            List<ProductIngredient> candidateIngredients,
            List<RoutineProductContext> currentProducts,
            LlmCompatibilityResponseDto analysis,
            ProductIngredientMatchDto ingredientMatch
    ) {
        CompatibilityStatus status = analysis.status() == null
                ? CompatibilityStatus.UNKNOWN
                : analysis.status();
        RecommendStatus recommended = status == CompatibilityStatus.GOOD
                ? RecommendStatus.KEEP
                : RecommendStatus.REMOVE;

        return new CompatibilityResponseDto(
                candidate.getId(),
                StringUtils.hasText(analysis.productName()) ? analysis.productName().strip() : "새 제품",
                recommended,
                buildRecommendReason(candidateIngredients, currentProducts, safeText(analysis.summary(), defaultSummary(status))),
                ingredientMatch.ingredients(),
                ingredientMatch.allergics()
        );
    }

    /** 루틴에 포함된 제품 전체와 겹치는 성분 개수를 나열하고, 마지막 줄에 LLM 요약을 덧붙인다. */
    private String buildRecommendReason(
            List<ProductIngredient> candidateIngredients,
            List<RoutineProductContext> currentProducts,
            String summary
    ) {
        List<String> lines = new ArrayList<>();
        for (RoutineProductContext context : currentProducts) {
            List<String> overlapping = overlappingIngredients(candidateIngredients, context.ingredients());
            if (!overlapping.isEmpty()) {
                lines.add("제품 " + context.product().getId() + "번과 " + overlapping.size() + "개 성분 중복");
            }
        }
        lines.add(summary);
        return String.join("\n", lines);
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
                .append("category: ").append(candidate.getCategory().describe()).append('\n')
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
                    .append("category: ").append(product.getCategory().describe()).append('\n')
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

    private record RoutineProductContext(
            Product product,
            List<RoutineSlot> slots,
            List<ProductIngredient> ingredients
    ) {
    }
}
