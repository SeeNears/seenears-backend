package com.seenears.letters.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.letters.dto.response.ReadLetterResponse;
import com.seenears.letters.service.LettersService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/letters")
public class LettersController {

    private static final String READ_SUCCESS_MESSAGE = "편지가 읽음 처리되었습니다.";

    private final LettersService lettersService;

    public LettersController(LettersService lettersService) {
        this.lettersService = lettersService;
    }

    @PatchMapping("/{letterId}/read")
    public ApiResponse<ReadLetterResponse> markAsRead(
            Authentication authentication,
            @PathVariable Long letterId
    ) {
        ReadLetterResponse response = lettersService.markAsRead(authentication.getName(), letterId);

        return ApiResponse.success(READ_SUCCESS_MESSAGE, response);
    }
}
