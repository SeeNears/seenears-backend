package com.seenears.internal.dto.response;

import com.seenears.global.domain.MoodType;
import com.seenears.voicerecords.domain.VoiceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PendingSttVoiceRecordsResponse(
        List<VoiceRecordResponse> voiceRecords
) {

    public static PendingSttVoiceRecordsResponse from(List<VoiceRecord> voiceRecords) {
        return new PendingSttVoiceRecordsResponse(
                voiceRecords.stream()
                        .map(VoiceRecordResponse::from)
                        .toList()
        );
    }

    public record VoiceRecordResponse(
            Long voiceRecordId,
            Long dailyRecordId,
            Long userId,
            String audioUrl,
            Integer durationSeconds,
            LocalDate recordDate,
            MoodType moodType,
            String questionText,
            LocalDateTime createdAt
    ) {

        public static VoiceRecordResponse from(VoiceRecord voiceRecord) {
            return new VoiceRecordResponse(
                    voiceRecord.getId(),
                    voiceRecord.getDailyRecord().getId(),
                    voiceRecord.getAppUser().getId(),
                    voiceRecord.getAudioUrl(),
                    voiceRecord.getDurationSeconds(),
                    voiceRecord.getDailyRecord().getRecordDate(),
                    voiceRecord.getDailyRecord().getMoodType(),
                    voiceRecord.getDailyRecord().getQuestionText(),
                    voiceRecord.getCreatedAt()
            );
        }
    }
}
