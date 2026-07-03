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
    DEFAULT_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_001", "활성 기본 질문을 찾을 수 없습니다."),
    DAILY_RECORD_ALREADY_EXISTS(HttpStatus.CONFLICT, "DAILY_RECORD_001", "오늘 이미 기록을 생성했습니다."),
    DAILY_RECORD_TIME_NOT_ALLOWED(HttpStatus.FORBIDDEN, "DAILY_RECORD_002", "기록 가능한 시간이 아닙니다."),
    DAILY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "DAILY_RECORD_003", "하루 기록을 찾을 수 없습니다."),
    DAILY_RECORD_STATUS_NOT_ALLOWED(HttpStatus.CONFLICT, "DAILY_RECORD_004", "현재 상태에서는 음성 기록을 제출할 수 없습니다."),
    DAILY_RECORD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DAILY_RECORD_005", "다른 사용자의 하루 기록입니다."),
    VOICE_RECORD_ALREADY_EXISTS(HttpStatus.CONFLICT, "VOICE_RECORD_001", "이미 음성 기록을 제출했습니다."),
    VOICE_RECORD_INVALID_DURATION(HttpStatus.BAD_REQUEST, "VOICE_RECORD_002", "음성 길이가 올바르지 않습니다."),
    VOICE_RECORD_FILE_EMPTY(HttpStatus.BAD_REQUEST, "VOICE_RECORD_003", "음성 파일은 필수입니다."),
    VOICE_RECORD_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "VOICE_RECORD_004", "지원하지 않는 음성 파일 형식입니다."),
    VOICE_RECORD_FILE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "VOICE_RECORD_005", "음성 파일 저장에 실패했습니다."),
    VOICE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "VOICE_RECORD_006", "음성 기록을 찾을 수 없습니다."),
    VOICE_RECORD_STT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "VOICE_RECORD_007", "이미 STT 처리가 완료된 음성 기록입니다."),
    PUSH_DEVICE_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "PUSH_DEVICE_TOKEN_001", "디바이스 토큰을 찾을 수 없습니다."),
    AI_ANALYSIS_STT_NOT_COMPLETED(HttpStatus.CONFLICT, "AI_ANALYSIS_001", "STT 처리가 완료되지 않아 AI 분석 결과를 저장할 수 없습니다."),
    AI_ANALYSIS_ALREADY_COMPLETED(HttpStatus.CONFLICT, "AI_ANALYSIS_002", "이미 AI 분석이 완료된 하루 기록입니다."),
    QUESTION_GENERATION_AI_ANALYSIS_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "QUESTION_GENERATION_001",
            "AI 분석이 완료되지 않아 다음 질문을 저장할 수 없습니다."
    ),
    QUESTION_GENERATION_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "QUESTION_GENERATION_002",
            "이미 다음 질문 생성이 완료된 하루 기록입니다."
    ),
    LETTER_NOT_FOUND(HttpStatus.NOT_FOUND, "LETTER_001", "편지를 찾을 수 없습니다."),
    LETTER_ALREADY_GENERATED(HttpStatus.CONFLICT, "LETTER_002", "이미 생성 완료된 편지입니다."),
    LETTER_VOICE_RECORD_MISMATCH(HttpStatus.CONFLICT, "LETTER_003", "편지와 음성 기록이 일치하지 않습니다."),
    LETTER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "LETTER_004", "다른 사용자의 편지입니다."),
    LETTER_STATUS_NOT_ALLOWED(HttpStatus.CONFLICT, "LETTER_005", "현재 상태에서는 편지를 읽음 처리할 수 없습니다.");

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
