package com.nyyb.nyybserver.common.response;

import lombok.Getter;

@Getter
public abstract class GlobalException extends RuntimeException {
    private final ErrorCode errorCode;

    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}