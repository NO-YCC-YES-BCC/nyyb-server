package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;
import java.util.UUID;

public record CompatibilityResponseDto(
        Long productId,
        String productName,
        ProductCategory category,
        int ingredientCount,
        List<OcrIngredientDto> ingredients,
        UUID routineId,
        CompatibilityStatus status,
        int score,
        RoutineSlot recommendedSlot,
        String summary,
        String usageGuide,
        List<CompatibilityIssueDto> issues,
        String disclaimer
) {
}
