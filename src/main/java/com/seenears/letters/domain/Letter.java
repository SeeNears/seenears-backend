package com.seenears.letters.domain;

import com.seenears.auth.domain.AppUser;
import com.seenears.dailyrecords.domain.DailyRecord;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "letters",
        uniqueConstraints = @UniqueConstraint(name = "uk_letters_daily_record_id", columnNames = "daily_record_id")
)
public class Letter {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyRecord dailyRecord;

    @Column(name = "prompt_version_id")
    private Long promptVersionId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDate letterDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private LetterStatus status = LetterStatus.PENDING;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column
    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean fallbackUsed = false;

    @Column
    private LocalDateTime generatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Letter() {
    }

    public static Letter create(DailyRecord dailyRecord) {
        Letter letter = new Letter();
        letter.appUser = dailyRecord.getAppUser();
        letter.dailyRecord = dailyRecord;
        letter.letterDate = dailyRecord.getRecordDate();
        letter.status = LetterStatus.PENDING;
        letter.read = false;
        letter.fallbackUsed = false;
        return letter;
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

    public AppUser getAppUser() {
        return appUser;
    }

    public DailyRecord getDailyRecord() {
        return dailyRecord;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getLetterDate() {
        return letterDate;
    }

    public LetterStatus getStatus() {
        return status;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void saveGenerated(String content, boolean fallbackUsed) {
        this.status = fallbackUsed ? LetterStatus.FALLBACK_GENERATED : LetterStatus.GENERATED;
        this.content = content;
        this.fallbackUsed = fallbackUsed;
        this.generatedAt = LocalDateTime.now(SERVICE_ZONE);
    }

    public void saveFailed() {
        this.status = LetterStatus.FAILED;
        this.content = null;
        this.fallbackUsed = false;
        this.generatedAt = LocalDateTime.now(SERVICE_ZONE);
    }
}
