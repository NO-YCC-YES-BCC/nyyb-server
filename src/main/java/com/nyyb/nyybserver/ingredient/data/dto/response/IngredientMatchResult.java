package com.nyyb.nyybserver.ingredient.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchStatus;
import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchType;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class IngredientMatchResult {
    private String inputName;
    private IngredientMatchType matchType;
    private String matchedAlias;
    private Long ingredientId;
    private String ingredientName;
    private Boolean isToxic;
    private RiskLevel riskLevel;
    private String riskLabel;
    private Boolean regulated;
    private IngredientMatchStatus matchStatus;
    private String description;
    private List<IngredientMatchCandidateDto> candidates;
    private List<IngredientMatchCandidateDto> ambiguousCandidates;
}
