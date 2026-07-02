package com.seenears.aianalysis.repository;

import com.seenears.aianalysis.domain.AiAnalysisResult;
import com.seenears.dailyrecords.domain.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {

    Optional<AiAnalysisResult> findByDailyRecord(DailyRecord dailyRecord);
}
