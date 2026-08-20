package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

// 분석 응답의 제품 1개. LLM 응답(1단계 제품명 + 2단계 판단)을 합쳐 만들고, 상세 조회에서는 저장된 Product로 만든다.
public record AnalysisProductDto(
        Long productId,
        String productName,
        RecommendStatus recommended, // KEEP / REMOVE
        String recommendReason
) {
}
