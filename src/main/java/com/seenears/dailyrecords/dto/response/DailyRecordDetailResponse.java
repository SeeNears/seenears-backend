package com.seenears.dailyrecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.global.domain.MoodType;
import com.seenears.letters.domain.Letter;

import java.time.LocalDate;

public record DailyRecordDetailResponse(
        Long dailyRecordId,
        LocalDate recordDate,
        MoodType moodType,
        String questionText,
        DailyRecordStatus status,
        boolean hasVoice,
        LetterResponse letter
) {

    public static DailyRecordDetailResponse of(DailyRecord dailyRecord, boolean hasVoice, Letter letter) {
        return new DailyRecordDetailResponse(
                dailyRecord.getId(),
                dailyRecord.getRecordDate(),
                dailyRecord.getMoodType(),
                dailyRecord.getQuestionText(),
                dailyRecord.getStatus(),
                hasVoice,
                LetterResponse.from(letter)
        );
    }

    public record LetterResponse(
            Long letterId,
            String status,
            String content,
            boolean isRead,
            boolean fallbackUsed
    ) {

        public static LetterResponse from(Letter letter) {
            if (letter == null) {
                return null;
            }

            return new LetterResponse(
                    letter.getId(),
                    letter.getStatus().name(),
                    letter.getContent(),
                    letter.isRead(),
                    letter.isFallbackUsed()
            );
        }
    }
}
