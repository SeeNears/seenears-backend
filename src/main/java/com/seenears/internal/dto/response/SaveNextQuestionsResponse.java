package com.seenears.internal.dto.response;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionGenerationStatus;

import java.time.LocalDateTime;

public record SaveNextQuestionsResponse(
        Long dailyRecordId,
        QuestionGenerationStatus questionGenerationStatus,
        LocalDateTime questionGeneratedAt
) {

    public static SaveNextQuestionsResponse from(DailyRecord dailyRecord) {
        return new SaveNextQuestionsResponse(
                dailyRecord.getId(),
                dailyRecord.getQuestionGenerationStatus(),
                dailyRecord.getQuestionGeneratedAt()
        );
    }
}
