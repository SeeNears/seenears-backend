package com.seenears.internal.dto.request;

import com.seenears.voicerecords.domain.SttStatus;
import jakarta.validation.constraints.NotNull;

public record SaveSttResultRequest(
        @NotNull
        SttStatus sttStatus,

        String sttText
) {
}
