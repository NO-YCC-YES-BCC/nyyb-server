package com.nyyb.nyybserver.routine.data.dto.response;

import java.util.List;
import java.util.UUID;

public record CreateRoutineResponseDto(
        UUID routineId,         // 루틴 식별자
        Integer score,          // 현재 루틴 100분위 점수
        String scoreReason,     // 점수 이유 문구
        String summary,         // 루틴 요약 문구
        List<RoutineProductDto> morning,  // 오전 루틴 제품
        List<RoutineProductDto> evening   // 오후(저녁) 루틴 제품
) {
}
