package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.product.data.dto.response.LlmProductIngredientsDto;

public interface ProductIngredientFinder {

    LlmProductIngredientsDto find(String itemName);
}
