package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

public record LlmCompatibilityIssueDto(
        RoutineSlot slot,
        Long routineProductId,
        String reason
) {
}
