package com.seenears.internal.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.internal.dto.request.SaveLetterResultRequest;
import com.seenears.internal.dto.response.SaveLetterResultResponse;
import com.seenears.internal.service.InternalLettersService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/letters")
public class InternalLettersController {

    private static final String LETTER_RESULT_SUCCESS_MESSAGE = "편지 생성 결과가 저장되었습니다.";

    private final InternalLettersService internalLettersService;

    public InternalLettersController(InternalLettersService internalLettersService) {
        this.internalLettersService = internalLettersService;
    }

    @PatchMapping("/{letterId}")
    public ApiResponse<SaveLetterResultResponse> saveLetterResult(
            @PathVariable Long letterId,
            @Valid @RequestBody SaveLetterResultRequest request
    ) {
        SaveLetterResultResponse response = internalLettersService.saveLetterResult(letterId, request);

        return ApiResponse.success(LETTER_RESULT_SUCCESS_MESSAGE, response);
    }
}
