package com.seenears.dailyrecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.global.domain.MoodType;

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

    public static DailyRecordDetailResponse of(DailyRecord dailyRecord, boolean hasVoice) {
        return new DailyRecordDetailResponse(
                dailyRecord.getId(),
                dailyRecord.getRecordDate(),
                dailyRecord.getMoodType(),
                dailyRecord.getQuestionText(),
                dailyRecord.getStatus(),
                hasVoice,
                null
        );
    }

    public record LetterResponse(
            Long letterId,
            String status,
            String content,
            boolean isRead,
            boolean fallbackUsed
    ) {
    }
}
