package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

// required = true: strict 스키마는 required에 없는 필드의 누락·null을 허용하므로,
// 모든 필드를 required로 올려 recommendReason이 비어 오는 응답 자체를 모델이 만들 수 없게 한다.
public record LlmProductAnalysisDto(
        @JsonProperty(required = true) Long productId,
        @JsonProperty(required = true) String productName,
        @JsonProperty(required = true) RecommendStatus recommended, // KEEP / REMOVE
        @JsonProperty(required = true) String recommendReason
) {
}
