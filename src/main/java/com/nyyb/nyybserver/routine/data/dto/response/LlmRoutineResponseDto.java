package com.nyyb.nyybserver.routine.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LlmRoutineResponseDto(
        @JsonProperty(required = true) Integer score,        // 현재 루틴 100분위 점수
        @JsonProperty(required = true) String scoreReason,   // 점수 이유 문구
        @JsonProperty(required = true) String summary,       // 루틴 요약 문구
        @JsonProperty(required = true) List<LlmRoutineItemDto> items
) {
}
