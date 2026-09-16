package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 제품명은 마스터 카탈로그(name_ko)에서 오므로 LLM에 제품명 확정을 시키지 않는다.
public record LlmAnalysisResponseDto(
        @JsonProperty(required = true) List<LlmProductAnalysisDto> products
) {
}
