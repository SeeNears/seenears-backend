package com.seenears.voicerecords.repository;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.voicerecords.domain.VoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    boolean existsByDailyRecord(DailyRecord dailyRecord);

    @Query("select vr.dailyRecord.id from VoiceRecord vr where vr.dailyRecord in :dailyRecords")
    Set<Long> findDailyRecordIdsByDailyRecordIn(@Param("dailyRecords") List<DailyRecord> dailyRecords);
}
