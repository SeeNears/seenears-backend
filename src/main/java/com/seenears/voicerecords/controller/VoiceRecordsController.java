package com.seenears.voicerecords.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.voicerecords.dto.request.CreateVoiceRecordRequest;
import com.seenears.voicerecords.dto.response.CreateVoiceRecordResponse;
import com.seenears.voicerecords.service.VoiceRecordsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-records/{dailyRecordId}/voice-records")
public class VoiceRecordsController {

    private static final String CREATE_SUCCESS_MESSAGE = "음성 기록이 업로드되었습니다.";

    private final VoiceRecordsService voiceRecordsService;

    public VoiceRecordsController(VoiceRecordsService voiceRecordsService) {
        this.voiceRecordsService = voiceRecordsService;
    }

    @PostMapping
    public ApiResponse<CreateVoiceRecordResponse> createVoiceRecord(
            Authentication authentication,
            @PathVariable Long dailyRecordId,
            @ModelAttribute CreateVoiceRecordRequest request
    ) {
        CreateVoiceRecordResponse response = voiceRecordsService.createVoiceRecord(
                authentication.getName(),
                dailyRecordId,
                request
        );

        return ApiResponse.success(CREATE_SUCCESS_MESSAGE, response);
    }
}
