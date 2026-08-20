package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// productNames가 products보다 먼저 생성돼야 한다. 모델이 모든 제품명을 확정한 뒤에야
// products의 recommendReason에서 그 제품명으로 다른 제품을 지칭할 수 있기 때문이다.
// 생성 순서는 strict 스키마의 프로퍼티 순서를 따르고, 그 순서는 필드 선언 순이 아니라 이름 오름차순이다.
// ("productNames" < "products") 필드 이름을 바꾸면 순서가 뒤집힐 수 있으므로 스키마 순서를 함께 확인한다.
public record LlmAnalysisResponseDto(
        @JsonProperty(required = true) List<LlmProductNameDto> productNames,
        @JsonProperty(required = true) List<LlmProductAnalysisDto> products
) {
}
