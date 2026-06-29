package com.seenears.dailyrecords.domain;

import com.seenears.auth.domain.AppUser;
import com.seenears.global.domain.MoodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Entity
@Table(
        name = "daily_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_records_user_record_date", columnNames = {"user_id", "record_date"})
)
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MoodType moodType;

    @Column(nullable = false, length = 500)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSource questionSource = QuestionSource.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DailyRecordStatus status = DailyRecordStatus.QUESTION_ASSIGNED;

    @Column(length = 500)
    private String nextQuestionSunny;

    @Column(length = 500)
    private String nextQuestionCloudy;

    @Column(length = 500)
    private String nextQuestionRainy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionGenerationStatus questionGenerationStatus = QuestionGenerationStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected DailyRecord() {
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public MoodType getMoodType() {
        return moodType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public QuestionGenerationStatus getQuestionGenerationStatus() {
        return questionGenerationStatus;
    }

    public Map<MoodType, String> getNextQuestions() {
        Map<MoodType, String> questions = new EnumMap<>(MoodType.class);
        questions.put(MoodType.SUNNY, nextQuestionSunny);
        questions.put(MoodType.CLOUDY, nextQuestionCloudy);
        questions.put(MoodType.RAINY, nextQuestionRainy);
        return questions;
    }

    public boolean hasAllNextQuestions() {
        return hasText(nextQuestionSunny) && hasText(nextQuestionCloudy) && hasText(nextQuestionRainy);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
