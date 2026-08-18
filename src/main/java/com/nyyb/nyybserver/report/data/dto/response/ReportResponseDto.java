package com.nyyb.nyybserver.report.data.dto.response;

import com.nyyb.nyybserver.report.data.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponseDto(
        UUID reportId,
        ReportStatus status,
        String title,
        LocalDateTime createdAt,
        Integer score,
        String scoreReason,
        String summary,
        ReportProductCountDto productCount,
        ReportDayDto morning,
        ReportDayDto evening
) {
}
