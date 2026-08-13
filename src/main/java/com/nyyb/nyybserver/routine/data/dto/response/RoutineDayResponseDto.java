package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;

public record RoutineDayResponseDto(
        RoutineSlot slot,                    // 요청 시간대 (MORNING / EVENING)
        List<RoutineDayProductDto> products  // userRoutineSlot 기준 해당 시간대 제품 목록
) {
}
