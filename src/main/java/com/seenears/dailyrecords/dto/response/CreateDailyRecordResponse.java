package com.seenears.dailyrecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.global.domain.MoodType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateDailyRecordResponse(
        Long dailyRecordId,
        LocalDate recordDate,
        MoodType moodType,
        String questionText,
        MoodType questionUsed,
        DailyRecordStatus status,
        LocalDateTime createdAt
) {

    public static CreateDailyRecordResponse from(DailyRecord dailyRecord) {
        return new CreateDailyRecordResponse(
                dailyRecord.getId(),
                dailyRecord.getRecordDate(),
                dailyRecord.getMoodType(),
                dailyRecord.getQuestionText(),
                dailyRecord.getMoodType(),
                dailyRecord.getStatus(),
                dailyRecord.getCreatedAt()
        );
    }
}
