package com.nyyb.nyybserver.common.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<GlobalResponse<?>> handleGlobalException(GlobalException e) {
        log.warn("[GlobalException] {} - {}", e.getErrorCode().getCode(), e.getMessage());
        return toResponse(e.getErrorCode());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<GlobalResponse<?>> handleBadRequest(Exception e) {
        log.warn("[BadRequest] {} - {}", e.getClass().getSimpleName(), e.getMessage());
        return toResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GlobalResponse<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotAllowed] {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(GlobalResponse.error(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponse<?>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDenied] {}", e.getMessage());
        return toResponse(ErrorCode.ACCESS_DENIED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleUnknown(Exception e) {
        log.error("[UnknownError]", e);
        return toResponse(ErrorCode.UNKNOWN_ERROR);
    }

    private ResponseEntity<GlobalResponse<?>> toResponse(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(GlobalResponse.error(errorCode));
    }
}