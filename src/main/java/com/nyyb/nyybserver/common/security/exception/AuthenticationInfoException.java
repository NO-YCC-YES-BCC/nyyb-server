package com.nyyb.nyybserver.common.security.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class AuthenticationInfoException extends GlobalException {
    public AuthenticationInfoException() {
        super(ErrorCode.VERIFICATION_INVALID);
    }
}
