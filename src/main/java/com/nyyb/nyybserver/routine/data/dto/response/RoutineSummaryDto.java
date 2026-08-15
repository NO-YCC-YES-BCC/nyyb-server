package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.routine.data.entity.Routine;

import java.util.UUID;

public record RoutineSummaryDto(
        UUID id,
        String title
) {
    public static RoutineSummaryDto from(Routine routine) {
        return new RoutineSummaryDto(
                routine.getId(),
                routine.getTitle()
        );
    }
}
