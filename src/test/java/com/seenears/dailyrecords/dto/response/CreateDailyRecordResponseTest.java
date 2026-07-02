package com.seenears.dailyrecords.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CreateDailyRecordResponseTest {

    @Test
    void createDailyRecordResponseDoesNotExposeQuestionUsed() throws JsonProcessingException {
        DailyRecord dailyRecord = DailyRecord.create(
                new AppUser("테스터", "01000000000", UserStatus.ACTIVE),
                LocalDate.of(2026, 6, 23),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", 10L);
        ReflectionTestUtils.setField(dailyRecord, "createdAt", LocalDateTime.of(2026, 6, 23, 18, 10));

        String json = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(CreateDailyRecordResponse.from(dailyRecord));

        assertThat(json).contains("\"dailyRecordId\":10");
        assertThat(json).contains("\"moodType\":\"SUNNY\"");
        assertThat(json).contains("\"questionText\":\"오늘 기분이 좋으셨던 이유가 있을까요?\"");
        assertThat(json).doesNotContain("questionUsed");
    }
}
