package com.nyyb.nyybserver.routine.data.dto.response;

import java.util.List;
import java.util.UUID;

public record RoutineDesignResponseDto(
        UUID routineId,         // 루틴 식별자
        String title,           // 한국 날짜 + 제품 개수 문구 (예: "8월 3일 5개의 제품")
        Integer score,          // 현재 루틴 100분위 점수
        String scoreReason,     // 점수 이유 문구
        String summary,         // 루틴 요약 문구
        List<RoutineProductDto> morning,  // 오전 루틴 제품
        List<RoutineProductDto> evening   // 오후(저녁) 루틴 제품
) {
}
