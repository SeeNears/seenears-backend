package com.seenears.internal.dto.request;

import com.seenears.dailyrecords.domain.QuestionGenerationStatus;
import jakarta.validation.constraints.NotNull;

public record SaveNextQuestionsRequest(
        @NotNull
        QuestionGenerationStatus questionGenerationStatus,

        String nextQuestionSunny,

        String nextQuestionCloudy,

        String nextQuestionRainy
) {
}
