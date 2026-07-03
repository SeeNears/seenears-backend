package com.seenears.push.domain;

import com.seenears.auth.domain.AppUser;
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

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "push_device_tokens")
public class PushDeviceToken {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Column(nullable = false, unique = true, length = 500)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private DeviceType deviceType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected PushDeviceToken() {
    }

    public PushDeviceToken(AppUser appUser, String deviceToken, DeviceType deviceType) {
        this.appUser = appUser;
        this.deviceToken = deviceToken;
        this.deviceType = deviceType;
        this.active = true;
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

    public String getDeviceToken() {
        return deviceToken;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void registerTo(AppUser appUser, DeviceType deviceType) {
        this.appUser = appUser;
        this.deviceType = deviceType;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
