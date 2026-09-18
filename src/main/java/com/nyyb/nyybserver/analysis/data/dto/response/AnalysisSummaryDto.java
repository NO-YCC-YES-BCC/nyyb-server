package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;

import java.util.UUID;

public record AnalysisSummaryDto(
        UUID id,
        String title,
        long productCount,
        long removeCount, // LLM이 REMOVE로 제안한 제품 수
        Integer score     // 루틴 점수 (100점 만점). 루틴 설계 전이면 null
) {
    public static AnalysisSummaryDto from(Analysis analysis, long productCount, long removeCount, Integer score) {
        return new AnalysisSummaryDto(
                analysis.getId(),
                analysis.getTitle(),
                productCount,
                removeCount,
                score
        );
    }
}
