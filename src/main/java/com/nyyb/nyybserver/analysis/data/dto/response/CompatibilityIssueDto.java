package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;

public record CompatibilityIssueDto(
        RoutineSlot slot,
        Long routineProductId,
        String routineProductName,
        int overlappingIngredientCount,
        List<String> overlappingIngredients,
        String reason
) {
}
