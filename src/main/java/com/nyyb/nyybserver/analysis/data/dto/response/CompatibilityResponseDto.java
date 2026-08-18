package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.ingredient.data.dto.response.AllergicDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientDto;

import java.util.List;

public record CompatibilityResponseDto(
        Long productId,
        String productName,
        RecommendStatus recommended,
        String recommendReason,
        List<IngredientDto> ingredients,
        List<AllergicDto> allergics
) {
}
