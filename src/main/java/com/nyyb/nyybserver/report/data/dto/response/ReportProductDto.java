package com.nyyb.nyybserver.report.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

import java.util.List;
import java.util.UUID;

public record ReportProductDto(
        UUID productId,         // userProductId
        String categoryMain,    // 카테고리 대분류 (한글 라벨)
        String categorySub,     // 카테고리 소분류 (한글 라벨)
        String productName,
        RecommendStatus recommended,
        String recommendReason,
        Boolean selected,
        List<ReportIngredientDto> ingredients
) {
}
