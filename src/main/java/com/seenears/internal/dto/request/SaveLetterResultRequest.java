package com.seenears.internal.dto.request;

import com.seenears.letters.domain.LetterStatus;
import jakarta.validation.constraints.NotNull;

public record SaveLetterResultRequest(
        @NotNull
        Long voiceRecordId,

        @NotNull
        LetterStatus status,

        String content,

        @NotNull
        Boolean fallbackUsed
) {
}
