package com.nyyb.nyybserver.ingredient.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;

public record IngredientSummaryDto(
        Long id,
        String name
) {
    public static IngredientSummaryDto from(Ingredient ingredient) {
        return new IngredientSummaryDto(
                ingredient.getId(),
                ingredient.getName()
        );
    }
}
