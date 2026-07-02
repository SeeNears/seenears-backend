package com.seenears.dailyrecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.global.domain.MoodType;
import com.seenears.letters.domain.Letter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record MonthlyDailyRecordsResponse(
        int year,
        int month,
        List<RecordResponse> records
) {

    public static MonthlyDailyRecordsResponse of(
            int year,
            int month,
            List<DailyRecord> dailyRecords,
            Set<Long> voiceSubmittedDailyRecordIds,
            Map<Long, Letter> lettersByDailyRecordId
    ) {
        List<RecordResponse> records = dailyRecords.stream()
                .map(dailyRecord -> RecordResponse.of(
                        dailyRecord,
                        voiceSubmittedDailyRecordIds.contains(dailyRecord.getId()),
                        lettersByDailyRecordId.get(dailyRecord.getId())
                ))
                .toList();

        return new MonthlyDailyRecordsResponse(year, month, records);
    }

    public record RecordResponse(
            Long dailyRecordId,
            LocalDate recordDate,
            MoodType moodType,
            DailyRecordStatus status,
            boolean hasVoice,
            boolean hasLetter,
            Long letterId,
            String letterStatus,
            Boolean letterRead
    ) {

        public static RecordResponse of(DailyRecord dailyRecord, boolean hasVoice, Letter letter) {
            return new RecordResponse(
                    dailyRecord.getId(),
                    dailyRecord.getRecordDate(),
                    dailyRecord.getMoodType(),
                    dailyRecord.getStatus(),
                    hasVoice,
                    letter != null,
                    letter == null ? null : letter.getId(),
                    letter == null ? null : letter.getStatus().name(),
                    letter == null ? null : letter.isRead()
            );
        }
    }
}
