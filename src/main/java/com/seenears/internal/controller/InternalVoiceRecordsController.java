package com.seenears.internal.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.internal.dto.request.SaveSttResultRequest;
import com.seenears.internal.dto.response.PendingSttVoiceRecordsResponse;
import com.seenears.internal.dto.response.SaveSttResultResponse;
import com.seenears.internal.service.InternalVoiceRecordsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/voice-records")
public class InternalVoiceRecordsController {

    private static final String PENDING_STT_SUCCESS_MESSAGE = "STT 처리 대기 음성 기록 조회에 성공했습니다.";
    private static final String STT_RESULT_SUCCESS_MESSAGE = "STT 결과가 저장되었습니다.";

    private final InternalVoiceRecordsService internalVoiceRecordsService;

    public InternalVoiceRecordsController(InternalVoiceRecordsService internalVoiceRecordsService) {
        this.internalVoiceRecordsService = internalVoiceRecordsService;
    }

    @GetMapping("/pending-stt")
    public ApiResponse<PendingSttVoiceRecordsResponse> getPendingSttVoiceRecords(
            @RequestParam(defaultValue = "10") int limit
    ) {
        PendingSttVoiceRecordsResponse response = internalVoiceRecordsService.getPendingSttVoiceRecords(limit);

        return ApiResponse.success(PENDING_STT_SUCCESS_MESSAGE, response);
    }

    @PatchMapping("/{voiceRecordId}/stt-result")
    public ApiResponse<SaveSttResultResponse> saveSttResult(
            @PathVariable Long voiceRecordId,
            @Valid @RequestBody SaveSttResultRequest request
    ) {
        SaveSttResultResponse response = internalVoiceRecordsService.saveSttResult(voiceRecordId, request);

        return ApiResponse.success(STT_RESULT_SUCCESS_MESSAGE, response);
    }
}
