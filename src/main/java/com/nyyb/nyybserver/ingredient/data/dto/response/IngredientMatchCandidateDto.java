package com.nyyb.nyybserver.ingredient.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IngredientMatchCandidateDto {
    private Long ingredientId;
    private String ingredientName;
    private Boolean isToxic;
    private RiskLevel riskLevel;
    private String riskLabel;
    private Boolean regulated;
    private String description;
}
