package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class InvalidImageException extends GlobalException {
    public InvalidImageException() {
        super(ErrorCode.INVALID_IMAGE);
    }
}
