package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

// 슬롯 정보는 응답 최상단 slot에만 있고, 제품 안에는 KEEP/REMOVE만 담는다.
public record RoutineDayProductDto(
        Long id,                        // productId
        ProductCategory category,       // 제품 카테고리
        String productName,
        RecommendStatus recommended,    // 이 슬롯 기준 KEEP / REMOVE
        String recommendReason          // LLM 이유 문구
) {
}
