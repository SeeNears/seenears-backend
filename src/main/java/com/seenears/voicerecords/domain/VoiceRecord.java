package com.seenears.voicerecords.domain;

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

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "voice_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_voice_records_daily_record_id", columnNames = "daily_record_id")
)
public class VoiceRecord {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyRecord dailyRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Column(nullable = false, length = 500)
    private String audioUrl;

    @Column
    private Integer durationSeconds;

    @Column(columnDefinition = "TEXT")
    private String sttText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private SttStatus sttStatus = SttStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected VoiceRecord() {
    }

    public static VoiceRecord create(
            DailyRecord dailyRecord,
            AppUser appUser,
            String audioUrl,
            Integer durationSeconds
    ) {
        VoiceRecord voiceRecord = new VoiceRecord();
        voiceRecord.dailyRecord = dailyRecord;
        voiceRecord.appUser = appUser;
        voiceRecord.audioUrl = audioUrl;
        voiceRecord.durationSeconds = durationSeconds;
        voiceRecord.sttStatus = SttStatus.PENDING;
        return voiceRecord;
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

    public AppUser getAppUser() {
        return appUser;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public SttStatus getSttStatus() {
        return sttStatus;
    }

    public String getSttText() {
        return sttText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void saveSttSuccess(String sttText) {
        this.sttStatus = SttStatus.SUCCESS;
        this.sttText = sttText;
    }

    public void saveSttFailure() {
        this.sttStatus = SttStatus.FAILED;
        this.sttText = null;
    }
}
