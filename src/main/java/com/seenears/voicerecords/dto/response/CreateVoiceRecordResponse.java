package com.seenears.voicerecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateVoiceRecordResponse(
        Long voiceRecordId,
        Long dailyRecordId,
        String audioUrl,
        Integer durationSeconds,
        SttStatus sttStatus,
        DailyRecordStatus dailyRecordStatus,
        Long letterId,
        LetterStatus letterStatus,
        LocalDate letterDate,
        LocalDateTime createdAt
) {

    public static CreateVoiceRecordResponse from(VoiceRecord voiceRecord, Letter letter) {
        return new CreateVoiceRecordResponse(
                voiceRecord.getId(),
                voiceRecord.getDailyRecord().getId(),
                voiceRecord.getAudioUrl(),
                voiceRecord.getDurationSeconds(),
                voiceRecord.getSttStatus(),
                voiceRecord.getDailyRecord().getStatus(),
                letter.getId(),
                letter.getStatus(),
                letter.getLetterDate(),
                voiceRecord.getCreatedAt()
        );
    }
}
