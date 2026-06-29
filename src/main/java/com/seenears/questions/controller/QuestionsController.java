package com.seenears.questions.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.questions.dto.response.TodayQuestionsResponse;
import com.seenears.questions.service.QuestionsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionsController {

    private static final String QUESTIONS_SUCCESS_MESSAGE = "오늘 질문 조회에 성공했습니다.";
    private static final String ALREADY_RECORDED_MESSAGE = "오늘은 이미 기록을 시작했습니다.";

    private final QuestionsService questionsService;

    public QuestionsController(QuestionsService questionsService) {
        this.questionsService = questionsService;
    }

    @GetMapping("/today")
    public ApiResponse<TodayQuestionsResponse> getTodayQuestions(Authentication authentication) {
        TodayQuestionsResponse response = questionsService.getTodayQuestions(authentication.getName());
        String message = response.alreadyRecorded() ? ALREADY_RECORDED_MESSAGE : QUESTIONS_SUCCESS_MESSAGE;

        return ApiResponse.success(message, response);
    }
}
