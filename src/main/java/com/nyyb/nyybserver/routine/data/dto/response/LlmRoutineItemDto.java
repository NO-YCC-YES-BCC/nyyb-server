package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RoutineRecommendStatus;

public record LlmRoutineItemDto(
        Long productId,
        RoutineRecommendStatus recommended,  // 시간대+유지/제외 6종 (시간대는 여기서 도출)
        String recommendReason
) {
}
