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
    OTP_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_007", "인증번호 요청 횟수를 초과했습니다."),
    OTP_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_008", "휴대폰 인증이 완료되지 않았습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_009", "가입된 사용자를 찾을 수 없습니다."),
    USER_WITHDRAW_REQUESTED(HttpStatus.FORBIDDEN, "AUTH_010", "탈퇴 요청된 사용자입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_011", "Refresh Token이 올바르지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_012", "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_013", "유효한 Refresh Token을 찾을 수 없습니다."),
    DEFAULT_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_001", "활성 기본 질문을 찾을 수 없습니다.");

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
