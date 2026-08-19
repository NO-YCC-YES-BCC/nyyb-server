package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.routine.data.entity.Routine;

import java.util.UUID;

public record RoutineSummaryDto(
        UUID id,
        String title,
        long removeCount // RoutineItem.recommended == REMOVE 인 개수
) {
    public static RoutineSummaryDto from(Routine routine, long removeCount) {
        return new RoutineSummaryDto(
                routine.getId(),
                routine.getTitle(),
                removeCount
        );
    }
}
