package com.seenears.internal.dto.response;

import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;

import java.time.LocalDateTime;

public record SaveLetterResultResponse(
        Long letterId,
        Long voiceRecordId,
        LetterStatus status,
        boolean fallbackUsed,
        LocalDateTime generatedAt
) {

    public static SaveLetterResultResponse from(Letter letter, Long voiceRecordId) {
        return new SaveLetterResultResponse(
                letter.getId(),
                voiceRecordId,
                letter.getStatus(),
                letter.isFallbackUsed(),
                letter.getGeneratedAt()
        );
    }
}
