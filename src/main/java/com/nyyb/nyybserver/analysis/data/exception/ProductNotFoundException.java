package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class ProductNotFoundException extends GlobalException {
    public ProductNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
