package com.nyyb.nyybserver.ingredient.data.dto.response;

import java.util.List;

public record ProductIngredientMatchDto(
        Long productId,
        String productName,
        List<IngredientDto> ingredients, // 해당 제품에서 RiskLevel이 null이 아닌 성분 (중복 제거)
        List<AllergicDto> allergics      // 해당 제품 성분과 매칭된 알레르기 유발 물질 (중복 제거)
) {
}
