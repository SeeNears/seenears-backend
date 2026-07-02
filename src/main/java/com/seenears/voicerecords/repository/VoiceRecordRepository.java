package com.seenears.voicerecords.repository;

import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    boolean existsByDailyRecord(DailyRecord dailyRecord);

    @Query("select vr.dailyRecord.id from VoiceRecord vr where vr.dailyRecord in :dailyRecords")
    Set<Long> findDailyRecordIdsByDailyRecordIn(@Param("dailyRecords") List<DailyRecord> dailyRecords);

    @Query("""
            select vr
            from VoiceRecord vr
            join fetch vr.dailyRecord dr
            join fetch vr.appUser
            where vr.sttStatus = :sttStatus
              and dr.status = :dailyRecordStatus
            order by vr.createdAt asc, vr.id asc
            """)
    List<VoiceRecord> findPendingSttVoiceRecords(
            @Param("sttStatus") SttStatus sttStatus,
            @Param("dailyRecordStatus") DailyRecordStatus dailyRecordStatus,
            Pageable pageable
    );
}
