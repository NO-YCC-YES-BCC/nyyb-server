package com.nyyb.nyybserver.user.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class OAuthProcessException extends GlobalException {
    public OAuthProcessException() {
        super(ErrorCode.OAUTH_PROCESS_FAILED);
    }

    public OAuthProcessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
