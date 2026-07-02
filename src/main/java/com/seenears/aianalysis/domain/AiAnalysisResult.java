package com.seenears.aianalysis.domain;

import com.seenears.dailyrecords.domain.DailyRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Entity
@Table(
        name = "ai_analysis_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_analysis_results_daily_record_id",
                columnNames = "daily_record_id"
        )
)
public class AiAnalysisResult {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyRecord dailyRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private AiAnalysisStatus analysisStatus = AiAnalysisStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Convert(converter = KeywordsConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> keywords = List.of();

    @Column
    private LocalDateTime analyzedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected AiAnalysisResult() {
    }

    public static AiAnalysisResult create(DailyRecord dailyRecord) {
        AiAnalysisResult aiAnalysisResult = new AiAnalysisResult();
        aiAnalysisResult.dailyRecord = dailyRecord;
        aiAnalysisResult.analysisStatus = AiAnalysisStatus.PENDING;
        return aiAnalysisResult;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now(SERVICE_ZONE);
    }

    public Long getId() {
        return id;
    }

    public DailyRecord getDailyRecord() {
        return dailyRecord;
    }

    public AiAnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getKeywords() {
        return keywords == null ? List.of() : List.copyOf(keywords);
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void saveCompleted(String summary, List<String> keywords) {
        this.analysisStatus = AiAnalysisStatus.COMPLETED;
        this.summary = summary;
        this.keywords = List.copyOf(keywords);
        this.analyzedAt = LocalDateTime.now(SERVICE_ZONE);
    }

    public void saveFailed() {
        this.analysisStatus = AiAnalysisStatus.FAILED;
        this.summary = null;
        this.keywords = List.of();
        this.analyzedAt = null;
    }
}
