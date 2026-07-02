package com.seenears.internal.dto.response;

import com.seenears.global.domain.MoodType;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.voicerecords.domain.VoiceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PendingSttVoiceRecordsResponse(
        List<VoiceRecordResponse> voiceRecords
) {

    public static PendingSttVoiceRecordsResponse from(List<VoiceRecord> voiceRecords, Map<Long, Letter> lettersByDailyRecordId) {
        return new PendingSttVoiceRecordsResponse(
                voiceRecords.stream()
                        .map(voiceRecord -> VoiceRecordResponse.from(
                                voiceRecord,
                                lettersByDailyRecordId.get(voiceRecord.getDailyRecord().getId())
                        ))
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
            Long letterId,
            LetterStatus letterStatus,
            LocalDateTime createdAt
    ) {

        public static VoiceRecordResponse from(VoiceRecord voiceRecord, Letter letter) {
            return new VoiceRecordResponse(
                    voiceRecord.getId(),
                    voiceRecord.getDailyRecord().getId(),
                    voiceRecord.getAppUser().getId(),
                    voiceRecord.getAudioUrl(),
                    voiceRecord.getDurationSeconds(),
                    voiceRecord.getDailyRecord().getRecordDate(),
                    voiceRecord.getDailyRecord().getMoodType(),
                    voiceRecord.getDailyRecord().getQuestionText(),
                    letter == null ? null : letter.getId(),
                    letter == null ? null : letter.getStatus(),
                    voiceRecord.getCreatedAt()
            );
        }
    }
}
