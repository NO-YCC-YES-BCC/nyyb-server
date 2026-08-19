package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

public record LlmCompatibilityIssueDto(
        @JsonProperty(required = true) RoutineSlot slot,
        @JsonProperty(required = true) Long routineProductId,
        @JsonProperty(required = true) String reason
) {
}
