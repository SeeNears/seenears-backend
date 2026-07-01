package com.seenears.dailyrecords.repository;

import com.seenears.auth.domain.AppUser;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    Optional<DailyRecord> findByAppUserAndRecordDate(AppUser appUser, LocalDate recordDate);

    List<DailyRecord> findByAppUserAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
            AppUser appUser,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<DailyRecord> findTopByAppUserAndRecordDateLessThanAndQuestionGenerationStatusOrderByRecordDateDescCreatedAtDesc(
            AppUser appUser,
            LocalDate recordDate,
            QuestionGenerationStatus questionGenerationStatus
    );
}
