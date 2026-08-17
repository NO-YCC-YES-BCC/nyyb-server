package com.nyyb.nyybserver.report.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;

import java.util.List;

public record ReportDayDto(
        RoutineSlot slot,
        List<ReportProductDto> products
) {
}
