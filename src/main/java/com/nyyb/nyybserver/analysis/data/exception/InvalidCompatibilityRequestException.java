package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

/** productId 또는 routineId가 누락된 궁합 분석 요청이다. */
public class InvalidCompatibilityRequestException extends GlobalException {

    public InvalidCompatibilityRequestException() {
        super(ErrorCode.INVALID_PARAMETER);
    }
}
