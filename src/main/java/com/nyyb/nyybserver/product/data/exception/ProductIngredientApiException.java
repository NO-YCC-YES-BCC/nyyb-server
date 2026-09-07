package com.nyyb.nyybserver.product.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class ProductIngredientApiException extends GlobalException {
    public ProductIngredientApiException() {
        super(ErrorCode.PRODUCT_INGREDIENT_API_FAILED);
    }
}
