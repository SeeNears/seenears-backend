package com.seenears.internal.service;

import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveLetterResultRequest;
import com.seenears.internal.dto.response.SaveLetterResultResponse;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class InternalLettersService {

    private final LetterRepository letterRepository;
    private final VoiceRecordRepository voiceRecordRepository;

    public InternalLettersService(
            LetterRepository letterRepository,
            VoiceRecordRepository voiceRecordRepository
    ) {
        this.letterRepository = letterRepository;
        this.voiceRecordRepository = voiceRecordRepository;
    }

    @Transactional
    public SaveLetterResultResponse saveLetterResult(Long letterId, SaveLetterResultRequest request) {
        validateRequest(request);

        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LETTER_NOT_FOUND));
        VoiceRecord voiceRecord = voiceRecordRepository.findById(request.voiceRecordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_RECORD_NOT_FOUND));

        if (!Objects.equals(letter.getDailyRecord().getId(), voiceRecord.getDailyRecord().getId())) {
            throw new BusinessException(ErrorCode.LETTER_VOICE_RECORD_MISMATCH);
        }

        if (letter.getStatus() == LetterStatus.GENERATED
                || letter.getStatus() == LetterStatus.FALLBACK_GENERATED) {
            throw new BusinessException(ErrorCode.LETTER_ALREADY_GENERATED);
        }

        if (request.status() == LetterStatus.FAILED) {
            letter.saveFailed();
        } else {
            letter.saveGenerated(request.content().trim(), request.fallbackUsed());
        }

        return SaveLetterResultResponse.from(letter, voiceRecord.getId());
    }

    private void validateRequest(SaveLetterResultRequest request) {
        if (request == null || request.voiceRecordId() == null || request.status() == null
                || request.fallbackUsed() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.status() != LetterStatus.GENERATED
                && request.status() != LetterStatus.FALLBACK_GENERATED
                && request.status() != LetterStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if ((request.status() == LetterStatus.GENERATED
                || request.status() == LetterStatus.FALLBACK_GENERATED)
                && (request.content() == null || request.content().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.status() == LetterStatus.GENERATED && request.fallbackUsed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.status() == LetterStatus.FALLBACK_GENERATED && !request.fallbackUsed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.status() == LetterStatus.FAILED && request.fallbackUsed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
