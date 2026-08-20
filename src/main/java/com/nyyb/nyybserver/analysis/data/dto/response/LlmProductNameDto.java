package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// 1단계(제품명 확정) 결과. 2단계 분석의 recommendReason은 여기서 확정한 제품명으로만 다른 제품을 지칭한다.
public record LlmProductNameDto(
        @JsonProperty(required = true) Long productId,
        @JsonProperty(required = true) String productName
) {
}
