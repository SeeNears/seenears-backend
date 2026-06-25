package com.seenears.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_003", "이미 가입된 전화번호입니다."),
    OTP_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_004", "유효한 인증번호 요청이 없습니다."),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_005", "인증번호가 만료되었습니다."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "AUTH_006", "인증번호가 올바르지 않습니다."),
    OTP_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_007", "인증번호 요청 횟수를 초과했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
