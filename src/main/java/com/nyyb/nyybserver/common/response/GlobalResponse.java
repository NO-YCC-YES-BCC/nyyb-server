package com.nyyb.nyybserver.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalResponse<T> {
    private static final String DEFAULT_SUCCESS_MESSAGE = "성공";

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> GlobalResponse<T> ok(T data) {
        return new GlobalResponse<>(true, null, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> GlobalResponse<T> ok(String message, T data) {
        return new GlobalResponse<>(true, null, message, data);
    }

    public static GlobalResponse<Void> ok() {
        return new GlobalResponse<>(true, null, DEFAULT_SUCCESS_MESSAGE, null);
    }

    public static <T> GlobalResponse<T> error(ErrorCode errorCode) {
        return new GlobalResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> GlobalResponse<T> error(GlobalException e) {
        return error(e.getErrorCode());
    }
}