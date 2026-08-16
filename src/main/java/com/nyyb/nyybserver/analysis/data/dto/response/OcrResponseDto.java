package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OcrResponseDto {
    private Long productId;
    private ProductCategory category;
    private int ingredientCount; // 매칭된 성분 갯수
    private List<OcrIngredientDto> ingredients;
}
