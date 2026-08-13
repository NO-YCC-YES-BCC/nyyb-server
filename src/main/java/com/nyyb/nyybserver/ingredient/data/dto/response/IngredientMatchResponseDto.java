package com.nyyb.nyybserver.ingredient.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class IngredientMatchResponseDto {
    private int totalInputCount;
    private int resultCount;
    private int matchedCount;
    private int regulatedIngredientCount;
    private int notFoundCount;
    private int ambiguousCount;
    private List<String> notFoundNames;
    private List<String> ambiguousNames;
    private List<IngredientMatchResult> results;
}
