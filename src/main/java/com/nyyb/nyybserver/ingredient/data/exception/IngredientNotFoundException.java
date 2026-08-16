package com.nyyb.nyybserver.ingredient.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class IngredientNotFoundException extends GlobalException {
    public IngredientNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
