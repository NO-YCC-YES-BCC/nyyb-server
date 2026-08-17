package com.nyyb.nyybserver.report.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

import java.util.List;

public record ReportProductDto(
        Long productId,
        ProductCategory category,
        String productName,
        RecommendStatus recommended,
        String recommendReason,
        Boolean selected,
        List<ReportIngredientDto> ingredients
) {
}
