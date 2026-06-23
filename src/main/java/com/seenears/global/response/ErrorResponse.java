package com.seenears.global.response;

import com.seenears.global.exception.ErrorCode;

import java.util.List;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        List<ValidationError> errors
) {

    public ErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ValidationError> errors) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), errors);
    }

    public record ValidationError(
            String field,
            String message
    ) {
    }
}
