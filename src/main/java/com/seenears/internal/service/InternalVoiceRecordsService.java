package com.seenears.internal.service;

import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveSttResultRequest;
import com.seenears.internal.dto.response.PendingSttVoiceRecordsResponse;
import com.seenears.internal.dto.response.SaveSttResultResponse;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InternalVoiceRecordsService {

    private static final int MIN_PENDING_STT_LIMIT = 1;
    private static final int MAX_PENDING_STT_LIMIT = 50;

    private final VoiceRecordRepository voiceRecordRepository;

    public InternalVoiceRecordsService(VoiceRecordRepository voiceRecordRepository) {
        this.voiceRecordRepository = voiceRecordRepository;
    }

    @Transactional(readOnly = true)
    public PendingSttVoiceRecordsResponse getPendingSttVoiceRecords(int limit) {
        validateLimit(limit);

        List<VoiceRecord> voiceRecords = voiceRecordRepository.findPendingSttVoiceRecords(
                SttStatus.PENDING,
                DailyRecordStatus.VOICE_SUBMITTED,
                PageRequest.of(0, limit)
        );
        return PendingSttVoiceRecordsResponse.from(voiceRecords);
    }

    @Transactional
    public SaveSttResultResponse saveSttResult(Long voiceRecordId, SaveSttResultRequest request) {
        validateSttResultRequest(request);

        VoiceRecord voiceRecord = voiceRecordRepository.findById(voiceRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_RECORD_NOT_FOUND));

        if (voiceRecord.getSttStatus() == SttStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_STT_ALREADY_COMPLETED);
        }

        if (request.sttStatus() == SttStatus.SUCCESS) {
            voiceRecord.saveSttSuccess(request.sttText().trim());
        } else {
            voiceRecord.saveSttFailure();
        }

        return SaveSttResultResponse.from(voiceRecord);
    }

    private void validateLimit(int limit) {
        if (limit < MIN_PENDING_STT_LIMIT || limit > MAX_PENDING_STT_LIMIT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateSttResultRequest(SaveSttResultRequest request) {
        if (request == null || request.sttStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.sttStatus() != SttStatus.SUCCESS && request.sttStatus() != SttStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.sttStatus() == SttStatus.SUCCESS
                && (request.sttText() == null || request.sttText().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
