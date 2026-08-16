package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;

public record RoutineProductDto(
        Long id,                    // productId
        ProductCategory category,   // 제품 카테고리
        String productName
) {
}
