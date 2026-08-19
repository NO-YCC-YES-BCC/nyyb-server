package com.nyyb.nyybserver.ingredient.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;

public record IngredientDto(
        Long id,
        String name,
        Boolean isToxic,
        RiskLevel riskLevel,
        String description
) {
    public static IngredientDto from(Ingredient ingredient) {
        RiskLevel riskLevel = ingredient.getRiskLevel();
        return new IngredientDto(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getIsToxic(),
                riskLevel,
                // 성분 테이블의 description이 아닌 RiskLevel별 고정 문구를 내려준다
                riskLevel == null ? null : riskLevel.getDescription()
        );
    }
}
