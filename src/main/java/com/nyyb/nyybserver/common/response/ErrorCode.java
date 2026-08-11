package com.nyyb.nyybserver.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 400
    INVALID_REQUEST("C001", "잘못된 요청입니다.", 400),
    INVALID_PARAMETER("C002", "유효하지 않은 파라미터입니다.", 400),
    INVALID_IMAGE("C003", "이미지 파일이 올바르지 않습니다.", 400),
    UNSUPPORTED_IMAGE_FORMAT("C004", "지원하지 않는 이미지 형식입니다.", 400),

    // 401
    VERIFICATION_INVALID("C4011", "인증 정보가 유효하지 않습니다.", 401),
    TOKEN_INVALID("C4012", "유효하지 않은 토큰입니다.", 401),
    TOKEN_EXPIRED("C4013", "만료된 토큰입니다.", 401),
    TOKEN_MALFORMED("C4014", "토큰 형식이 올바르지 않습니다.", 401),
    TOKEN_UNSUPPORTED("C4015", "지원되지 않는 토큰입니다.", 401),
    TOKEN_MISSING("C4016", "토큰이 제공되지 않았습니다.", 401),
    TOKEN_TYPE_MISMATCH("C4017", "토큰 타입이 올바르지 않습니다.", 401),
    REFRESH_TOKEN_NOT_FOUND("C4018", "저장된 Refresh Token을 찾을 수 없습니다.", 401),
    REFRESH_TOKEN_MISMATCH("C4019", "Refresh Token이 일치하지 않습니다.", 401),
    OAUTH_PROCESS_FAILED("C4020", "소셜 로그인 처리에 실패했습니다.", 401),

    // 403
    ACCESS_DENIED("C403", "승인되지 않은 사용자입니다.", 403),

    // 404
    USER_DATA_NOT_FOUND("C4041", "사용자를 찾을 수 없습니다.", 404),
    DATA_NOT_FOUND("C404", "정보를 불러올 수 없습니다.", 404),

    // 502
    OCR_API_FAILED("C5021", "OCR 서비스 호출에 실패했습니다.", 502),
    KAKAO_API_FAILED("C5022", "카카오 API 호출에 실패했습니다.", 502),

    // 500
    UNKNOWN_ERROR("C500", "오류가 발생하였습니다.", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
