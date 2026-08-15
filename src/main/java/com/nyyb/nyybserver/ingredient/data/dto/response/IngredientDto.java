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
        return new IngredientDto(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getIsToxic(),
                ingredient.getRiskLevel(),
                ingredient.getDescription()
        );
    }
}
