package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;

import java.util.UUID;

public record AnalysisSummaryDto(
        UUID id,
        String title
) {
    public static AnalysisSummaryDto from(Analysis analysis) {
        return new AnalysisSummaryDto(
                analysis.getId(),
                analysis.getTitle()
        );
    }
}
