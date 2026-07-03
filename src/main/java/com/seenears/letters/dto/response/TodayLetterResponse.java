package com.seenears.letters.dto.response;

import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodayLetterResponse(
        Long letterId,
        Long dailyRecordId,
        LocalDate recordDate,
        LocalDate letterDate,
        LetterStatus status,
        String content,
        boolean isRead,
        LocalDateTime readAt,
        boolean fallbackUsed,
        LocalDateTime generatedAt
) {

    public static TodayLetterResponse from(Letter letter) {
        return new TodayLetterResponse(
                letter.getId(),
                letter.getDailyRecord().getId(),
                letter.getDailyRecord().getRecordDate(),
                letter.getLetterDate(),
                letter.getStatus(),
                letter.getContent(),
                letter.isRead(),
                letter.getReadAt(),
                letter.isFallbackUsed(),
                letter.getGeneratedAt()
        );
    }
}
