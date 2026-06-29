package com.seenears.dailyrecords.controller;

import com.seenears.dailyrecords.dto.request.CreateDailyRecordRequest;
import com.seenears.dailyrecords.dto.response.CreateDailyRecordResponse;
import com.seenears.dailyrecords.service.DailyRecordsService;
import com.seenears.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-records")
public class DailyRecordsController {

    private static final String CREATE_SUCCESS_MESSAGE = "하루 기록이 생성되었습니다.";

    private final DailyRecordsService dailyRecordsService;

    public DailyRecordsController(DailyRecordsService dailyRecordsService) {
        this.dailyRecordsService = dailyRecordsService;
    }

    @PostMapping
    public ApiResponse<CreateDailyRecordResponse> createDailyRecord(
            Authentication authentication,
            @Valid @RequestBody CreateDailyRecordRequest request
    ) {
        CreateDailyRecordResponse response = dailyRecordsService.createDailyRecord(authentication.getName(), request);

        return ApiResponse.success(CREATE_SUCCESS_MESSAGE, response);
    }
}
