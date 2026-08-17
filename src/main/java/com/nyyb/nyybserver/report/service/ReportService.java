package com.nyyb.nyybserver.report.service;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.report.data.dto.response.ReportDayDto;
import com.nyyb.nyybserver.report.data.dto.response.ReportIngredientDto;
import com.nyyb.nyybserver.report.data.dto.response.ReportProductCountDto;
import com.nyyb.nyybserver.report.data.dto.response.ReportProductDto;
import com.nyyb.nyybserver.report.data.dto.response.ReportResponseDto;
import com.nyyb.nyybserver.report.data.enums.ReportStatus;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.entity.RoutineItemSelection;
import com.nyyb.nyybserver.routine.data.exception.RoutineNotFoundException;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;
    private final ProductIngredientRepository productIngredientRepository;

    public ReportResponseDto getReport(UUID reportId, Long userId) {
        Routine routine = routineRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        return buildReport(routine);
    }

    public ReportResponseDto getLatestReport(Long userId) {
        Routine routine = routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .orElseThrow(RoutineNotFoundException::new);

        return buildReport(routine);
    }

    private ReportResponseDto buildReport(Routine routine) {
        List<RoutineItem> items = routineItemRepository
                .findByRoutineIdWithProductAndSelections(routine.getId());

        ReportDayDto morning = buildDay(items, RoutineSlot.MORNING);
        ReportDayDto evening = buildDay(items, RoutineSlot.EVENING);
        ReportProductCountDto productCount = buildProductCount(items);

        return new ReportResponseDto(
                routine.getId(),
                routine.getScore() == null ? ReportStatus.PENDING : ReportStatus.READY,
                routine.getTitle(),
                routine.getCreatedAt(),
                routine.getScore(),
                routine.getScoreReason(),
                routine.getSummary(),
                productCount,
                morning,
                evening
        );
    }

    private ReportDayDto buildDay(List<RoutineItem> items, RoutineSlot slot) {
        List<ReportProductDto> products = items.stream()
                .filter(item -> matchesSlot(item.getUserRoutineSlot(), slot))
                .map(item -> toProduct(item, slot))
                .toList();

        return new ReportDayDto(slot, products);
    }

    private ReportProductDto toProduct(RoutineItem item, RoutineSlot slot) {
        Product product = item.getProduct();
        RecommendStatus recommendation = recommendationFor(item, slot);

        return new ReportProductDto(
                product.getId(),
                product.getCategory(),
                product.getProductName(),
                recommendation,
                recommendationReasonFor(item, recommendation),
                selectedFor(item, slot),
                ingredientsOf(product.getId())
        );
    }

    private RecommendStatus recommendationFor(RoutineItem item, RoutineSlot slot) {
        if (item.getRecommended() == null) {
            return null;
        }
        if (item.getRecommended() == RecommendStatus.KEEP) {
            return RecommendStatus.KEEP;
        }
        return matchesSlot(item.getLlmRoutineSlot(), slot)
                ? RecommendStatus.REMOVE
                : RecommendStatus.KEEP;
    }

    private String recommendationReasonFor(
            RoutineItem item,
            RecommendStatus recommendation
    ) {
        if (item.getRecommended() == RecommendStatus.REMOVE
                && recommendation == RecommendStatus.KEEP) {
            return null;
        }
        return item.getRecommendReason();
    }

    private Boolean selectedFor(RoutineItem item, RoutineSlot slot) {
        return item.getSelections().stream()
                .filter(selection -> selection.getSlot() == slot)
                .findFirst()
                .map(selection -> selection.getAction() == RecommendStatus.KEEP)
                .orElse(null);
    }

    private List<ReportIngredientDto> ingredientsOf(Long productId) {
        return productIngredientRepository.findByProductIdWithIngredient(productId).stream()
                .map(this::toIngredient)
                .toList();
    }

    private ReportIngredientDto toIngredient(ProductIngredient productIngredient) {
        Ingredient ingredient = productIngredient.getIngredient();
        if (ingredient == null) {
            return new ReportIngredientDto(
                    productIngredient.getRawName(),
                    false,
                    null,
                    false,
                    null
            );
        }

        return new ReportIngredientDto(
                ingredient.getName(),
                true,
                ingredient.getRiskLevel(),
                Boolean.TRUE.equals(ingredient.getIsToxic()),
                ingredient.getDescription()
        );
    }

    private ReportProductCountDto buildProductCount(List<RoutineItem> items) {
        boolean hasSavedSelections = items.stream()
                .anyMatch(item -> !item.getSelections().isEmpty());

        int selected = hasSavedSelections
                ? (int) items.stream().filter(this::hasKeptSelection).count()
                : (int) items.stream().filter(this::hasKeptRecommendation).count();

        return new ReportProductCountDto(
                items.size(),
                selected,
                Math.max(0, items.size() - selected)
        );
    }

    private boolean hasKeptSelection(RoutineItem item) {
        return item.getSelections().stream()
                .map(RoutineItemSelection::getAction)
                .anyMatch(action -> action == RecommendStatus.KEEP);
    }

    private boolean hasKeptRecommendation(RoutineItem item) {
        boolean usedInMorning = matchesSlot(item.getUserRoutineSlot(), RoutineSlot.MORNING);
        boolean usedInEvening = matchesSlot(item.getUserRoutineSlot(), RoutineSlot.EVENING);

        return usedInMorning && recommendationFor(item, RoutineSlot.MORNING) != RecommendStatus.REMOVE
                || usedInEvening && recommendationFor(item, RoutineSlot.EVENING) != RecommendStatus.REMOVE;
    }

    private boolean matchesSlot(RoutineSlot itemSlot, RoutineSlot targetSlot) {
        return itemSlot == targetSlot || itemSlot == RoutineSlot.BOTH;
    }
}
