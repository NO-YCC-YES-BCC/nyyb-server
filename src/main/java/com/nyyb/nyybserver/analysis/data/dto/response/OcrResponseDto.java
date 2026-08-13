package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OcrResponseDto {
    private Long productId;
    private String imageUrl;
    private ProductCategory category;
    private List<IngredientSummaryDto> ingredients;
}