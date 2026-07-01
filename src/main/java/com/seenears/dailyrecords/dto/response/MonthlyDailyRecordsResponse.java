package com.seenears.dailyrecords.dto.response;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.global.domain.MoodType;

import java.time.LocalDate;
import java.util.List;
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
            Set<Long> voiceSubmittedDailyRecordIds
    ) {
        List<RecordResponse> records = dailyRecords.stream()
                .map(dailyRecord -> RecordResponse.of(
                        dailyRecord,
                        voiceSubmittedDailyRecordIds.contains(dailyRecord.getId())
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

        public static RecordResponse of(DailyRecord dailyRecord, boolean hasVoice) {
            // TODO: When Letter entity/repository is implemented, map the letter row linked by daily_record_id here.
            return new RecordResponse(
                    dailyRecord.getId(),
                    dailyRecord.getRecordDate(),
                    dailyRecord.getMoodType(),
                    dailyRecord.getStatus(),
                    hasVoice,
                    false,
                    null,
                    null,
                    null
            );
        }
    }
}
