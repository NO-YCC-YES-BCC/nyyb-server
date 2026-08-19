package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;

import java.util.UUID;

public record AnalysisSummaryDto(
        UUID id,
        String title,
        long productCount,
        long removeCount // LLM이 REMOVE로 제안한 제품 수
) {
    public static AnalysisSummaryDto from(Analysis analysis, long productCount, long removeCount) {
        return new AnalysisSummaryDto(
                analysis.getId(),
                analysis.getTitle(),
                productCount,
                removeCount
        );
    }
}
