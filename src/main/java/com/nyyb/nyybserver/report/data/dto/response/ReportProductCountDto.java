package com.nyyb.nyybserver.report.data.dto.response;

public record ReportProductCountDto(
        int analyzed,
        int selected,
        int removed
) {
}
