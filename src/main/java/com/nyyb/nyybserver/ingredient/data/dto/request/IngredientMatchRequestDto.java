package com.nyyb.nyybserver.ingredient.data.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class IngredientMatchRequestDto {

    // 성분/알레르기 매칭 대상 제품 id 목록
    private List<Long> productIds;
}
