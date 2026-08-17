package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;

public record LlmCompatibilityResponseDto(
        String productName,
        CompatibilityStatus status,
        Integer score,
        RoutineSlot recommendedSlot,
        String summary,
        String usageGuide,
        List<LlmCompatibilityIssueDto> issues
) {
}
