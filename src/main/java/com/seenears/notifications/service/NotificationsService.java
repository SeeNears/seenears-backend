package com.seenears.notifications.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.dto.NotificationPageResponse;
import com.seenears.notifications.dto.NotificationResponse;
import com.seenears.notifications.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationsService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    public NotificationsService(
            NotificationRepository notificationRepository,
            AppUserRepository appUserRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(String authenticatedUserId, int page, int size) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("scheduledAt"), Sort.Order.desc("id"))
        );
        Page<NotificationResponse> notifications = notificationRepository.findByAppUserId(
                appUser.getId(),
                pageable
        ).map(NotificationResponse::from);

        return NotificationPageResponse.from(notifications);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(String authenticatedUserId, Long notificationId) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        Notification notification = notificationRepository.findByIdAndAppUserId(notificationId, appUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        return NotificationResponse.from(notification);
    }

    private AppUser getAuthenticatedUser(String authenticatedUserId) {
        Long userId = parseUserId(authenticatedUserId);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (appUser.getStatus() == UserStatus.WITHDRAW_REQUESTED) {
            throw new BusinessException(ErrorCode.USER_WITHDRAW_REQUESTED);
        }

        if (appUser.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return appUser;
    }

    private Long parseUserId(String authenticatedUserId) {
        try {
            return Long.valueOf(authenticatedUserId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
