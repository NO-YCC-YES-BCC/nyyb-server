package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

public record LlmRoutineItemDto(
        Long productId,
        RoutineSlot slot,             // 배치 시간대 (MORNING / EVENING / BOTH)
        RecommendStatus recommended,  // 유지/제외 (KEEP / REMOVE)
        String recommendReason
) {
}
