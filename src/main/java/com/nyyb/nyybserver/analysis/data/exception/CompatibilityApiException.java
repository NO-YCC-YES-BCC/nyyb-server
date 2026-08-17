package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class CompatibilityApiException extends GlobalException {

    public CompatibilityApiException() {
        super(ErrorCode.COMPATIBILITY_API_FAILED);
    }
}
