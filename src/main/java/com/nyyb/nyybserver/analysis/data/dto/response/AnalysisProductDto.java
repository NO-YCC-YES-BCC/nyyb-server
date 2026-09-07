package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

import java.util.UUID;

// 분석 응답의 제품 1개. 제품명은 마스터 카탈로그의 품목명을 그대로 쓴다.
public record AnalysisProductDto(
        UUID productId,              // userProductId
        String productName,
        RecommendStatus recommended, // KEEP / REMOVE
        String recommendReason
) {
}
