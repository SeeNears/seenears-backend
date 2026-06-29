package com.seenears.questions.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.global.domain.MoodType;

import java.time.LocalDate;
import java.util.Map;

public record TodayQuestionsResponse(
        QuestionCandidateSource source,
        LocalDate baseRecordDate,
        boolean alreadyRecorded,
        Long dailyRecordId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        MoodType questionUsed,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String questionText,
        Map<MoodType, String> questions
) {

    public static TodayQuestionsResponse recorded(DailyRecord dailyRecord) {
        return new TodayQuestionsResponse(
                QuestionCandidateSource.RECORDED,
                null,
                true,
                dailyRecord.getId(),
                dailyRecord.getMoodType(),
                dailyRecord.getQuestionText(),
                null
        );
    }

    public static TodayQuestionsResponse aiGenerated(LocalDate baseRecordDate, Map<MoodType, String> questions) {
        return new TodayQuestionsResponse(
                QuestionCandidateSource.AI_GENERATED,
                baseRecordDate,
                false,
                null,
                null,
                null,
                questions
        );
    }

    public static TodayQuestionsResponse defaults(Map<MoodType, String> questions) {
        return new TodayQuestionsResponse(
                QuestionCandidateSource.DEFAULT,
                null,
                false,
                null,
                null,
                null,
                questions
        );
    }
}
