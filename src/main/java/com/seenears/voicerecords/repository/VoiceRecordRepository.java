package com.seenears.voicerecords.repository;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.voicerecords.domain.VoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    boolean existsByDailyRecord(DailyRecord dailyRecord);
}
