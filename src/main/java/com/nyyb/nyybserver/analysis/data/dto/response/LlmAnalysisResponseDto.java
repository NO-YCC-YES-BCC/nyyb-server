package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LlmAnalysisResponseDto(
        @JsonProperty(required = true) List<LlmProductAnalysisDto> products
) {
}
