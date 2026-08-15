package com.nyyb.nyybserver.routine.data.dto.response;

import java.util.List;

public record LlmRoutineResponseDto(
        Integer score,        // 현재 루틴 100분위 점수
        String scoreReason,   // 점수 이유 문구
        String summary,       // 루틴 요약 문구
        List<LlmRoutineItemDto> items
) {
}
