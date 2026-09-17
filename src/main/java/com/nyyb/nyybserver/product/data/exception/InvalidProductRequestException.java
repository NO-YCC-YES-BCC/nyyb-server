package com.nyyb.nyybserver.product.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class InvalidProductRequestException extends GlobalException {
    public InvalidProductRequestException() {
        super(ErrorCode.INVALID_PARAMETER);
    }
}
