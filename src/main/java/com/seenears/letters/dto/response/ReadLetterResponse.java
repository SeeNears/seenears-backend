package com.seenears.letters.dto.response;

import com.seenears.letters.domain.Letter;

import java.time.LocalDateTime;

public record ReadLetterResponse(
        Long letterId,
        boolean isRead,
        LocalDateTime readAt
) {

    public static ReadLetterResponse from(Letter letter) {
        return new ReadLetterResponse(
                letter.getId(),
                letter.isRead(),
                letter.getReadAt()
        );
    }
}
