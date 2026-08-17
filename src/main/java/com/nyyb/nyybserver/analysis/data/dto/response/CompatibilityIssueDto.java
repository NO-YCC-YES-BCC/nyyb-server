package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;

public record CompatibilityIssueDto(
        RoutineSlot slot,
        Long routineProductId,
        String routineProductName,
        List<String> overlappingIngredients,
        String reason
) {
}
