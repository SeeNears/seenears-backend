package com.seenears.voicerecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;

import java.time.LocalDateTime;

public record CreateVoiceRecordResponse(
        Long voiceRecordId,
        Long dailyRecordId,
        String audioUrl,
        Integer durationSeconds,
        SttStatus sttStatus,
        DailyRecordStatus dailyRecordStatus,
        LocalDateTime createdAt
) {

    public static CreateVoiceRecordResponse from(VoiceRecord voiceRecord) {
        return new CreateVoiceRecordResponse(
                voiceRecord.getId(),
                voiceRecord.getDailyRecord().getId(),
                voiceRecord.getAudioUrl(),
                voiceRecord.getDurationSeconds(),
                voiceRecord.getSttStatus(),
                voiceRecord.getDailyRecord().getStatus(),
                voiceRecord.getCreatedAt()
        );
    }
}
