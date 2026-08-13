package com.nyyb.nyybserver.ingredient.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class IngredientMatchRequestDto {

    @Schema(description = "OCR or analysis extracted ingredient names")
    private List<String> ingredients;

    @Schema(description = "Raw OCR text. Used only when ingredients is empty.")
    private String rawText;
}
