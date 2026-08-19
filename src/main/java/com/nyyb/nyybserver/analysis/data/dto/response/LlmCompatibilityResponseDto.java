package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;

// required = true: strict 스키마는 required에 없는 필드의 누락·null을 허용하므로,
// 모든 필드를 required로 올려 summary·usageGuide가 비어 오는 응답 자체를 모델이 만들 수 없게 한다.
// (issues는 "문제 없으면 빈 배열"이라 required여도 프롬프트와 어긋나지 않는다)
public record LlmCompatibilityResponseDto(
        @JsonProperty(required = true) String productName,
        @JsonProperty(required = true) CompatibilityStatus status,
        @JsonProperty(required = true) Integer score,
        @JsonProperty(required = true) RoutineSlot recommendedSlot,
        @JsonProperty(required = true) String summary,
        @JsonProperty(required = true) String usageGuide,
        @JsonProperty(required = true) List<LlmCompatibilityIssueDto> issues
) {
}
