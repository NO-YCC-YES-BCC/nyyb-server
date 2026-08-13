package com.nyyb.nyybserver.ingredient.data.dto.response;

import java.util.List;

public record IngredientMatchResponseDto(
        List<IngredientDto> ingredients, // RiskLevel이 null이 아닌 성분 (중복 제거)
        List<AllergicDto> allergics      // 성분과 매칭된 알레르기 유발 물질 (중복 제거)
) {
}
