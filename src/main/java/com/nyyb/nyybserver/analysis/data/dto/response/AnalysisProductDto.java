package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

import java.util.UUID;

// 분석 응답의 제품 1개. 제품명은 마스터 카탈로그의 제품명을 그대로 쓴다.
public record AnalysisProductDto(
        UUID productId,              // userProductId. 루틴 수정 등 "이 분석에서 고른 제품"을 가리킬 때 쓴다
        Long masterProductId,        // 제품 마스터 id. 성분 조회(/ingredients/match)·검색 결과와 같은 id 체계
        String productName,
        RecommendStatus recommended, // KEEP / REMOVE
        String recommendReason
) {
}
