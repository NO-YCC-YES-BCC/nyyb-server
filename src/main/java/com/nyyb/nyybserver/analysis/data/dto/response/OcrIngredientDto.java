package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OcrIngredientDto {

    private Long ingredientId;
    private String name;
    private Boolean isToxic;
    private RiskLevel riskLevel;

}