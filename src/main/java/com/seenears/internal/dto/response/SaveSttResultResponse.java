package com.seenears.internal.dto.response;

import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;

public record SaveSttResultResponse(
        Long voiceRecordId,
        SttStatus sttStatus
) {

    public static SaveSttResultResponse from(VoiceRecord voiceRecord) {
        return new SaveSttResultResponse(
                voiceRecord.getId(),
                voiceRecord.getSttStatus()
        );
    }
}
