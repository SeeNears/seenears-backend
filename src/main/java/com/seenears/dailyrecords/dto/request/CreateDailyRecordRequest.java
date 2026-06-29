package com.seenears.dailyrecords.dto.request;

import com.seenears.global.domain.MoodType;
import jakarta.validation.constraints.NotNull;

public record CreateDailyRecordRequest(
        @NotNull(message = "moodType은 필수입니다.")
        MoodType moodType
) {
}
