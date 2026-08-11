package com.nyyb.nyybserver.common.security.exception;

public class AuthenticationInfoException extends RuntimeException {
    public AuthenticationInfoException() {
        super("사용자 정보를 찾을 수 없습니다.");
    }

    public AuthenticationInfoException(String message) {
        super(message);
    }

    public AuthenticationInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}
