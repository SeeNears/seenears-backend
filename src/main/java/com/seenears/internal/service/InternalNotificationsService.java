package com.seenears.internal.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.CreateNotificationRequest;
import com.seenears.internal.dto.request.SaveNotificationResultRequest;
import com.seenears.internal.dto.response.CreateNotificationResponse;
import com.seenears.internal.dto.response.SaveNotificationResultResponse;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
import com.seenears.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class InternalNotificationsService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final LetterRepository letterRepository;

    public InternalNotificationsService(
            NotificationRepository notificationRepository,
            AppUserRepository appUserRepository,
            LetterRepository letterRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
        this.letterRepository = letterRepository;
    }

    @Transactional
    public CreateNotificationResponse createNotification(CreateNotificationRequest request) {
        validateCreateRequest(request);

        AppUser appUser = appUserRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.type() == NotificationType.LETTER_ARRIVED) {
            Letter letterArrivedLetter = letterRepository.findById(request.letterId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LETTER_NOT_FOUND));
            validateLetterArrivedRequest(appUser, letterArrivedLetter);

            return notificationRepository.findFirstByLetterIdAndTypeOrderByIdAsc(
                            letterArrivedLetter.getId(),
                            NotificationType.LETTER_ARRIVED
                    )
                    .map(CreateNotificationResponse::from)
                    .orElseGet(() -> saveNotification(appUser, letterArrivedLetter, request));
        }

        return saveNotification(appUser, null, request);
    }

    @Transactional
    public SaveNotificationResultResponse saveNotificationResult(
            Long notificationId,
            SaveNotificationResultRequest request
    ) {
        validateResultRequest(request);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (request.status() == NotificationStatus.SENT) {
            LocalDateTime sentAt = request.sentAt() == null
                    ? LocalDateTime.now(SERVICE_ZONE)
                    : request.sentAt();
            notification.markSent(sentAt);
        } else if (request.status() == NotificationStatus.FAILED) {
            notification.markFailed();
        } else {
            throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_STATUS);
        }

        return SaveNotificationResultResponse.from(notification);
    }

    private CreateNotificationResponse saveNotification(
            AppUser appUser,
            Letter letter,
            CreateNotificationRequest request
    ) {
        Notification notification = Notification.create(
                appUser,
                letter,
                request.type(),
                request.title().trim(),
                request.body().trim(),
                request.scheduledAt()
        );

        return CreateNotificationResponse.from(notificationRepository.save(notification));
    }

    private void validateCreateRequest(CreateNotificationRequest request) {
        if (request == null
                || request.userId() == null
                || request.type() == null
                || request.title() == null
                || request.title().isBlank()
                || request.body() == null
                || request.body().isBlank()
                || request.scheduledAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.type() == NotificationType.LETTER_ARRIVED && request.letterId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateLetterArrivedRequest(AppUser appUser, Letter letter) {
        if (!Objects.equals(letter.getAppUser().getId(), appUser.getId())) {
            throw new BusinessException(ErrorCode.LETTER_ACCESS_DENIED);
        }

        if (letter.getStatus() != LetterStatus.GENERATED
                && letter.getStatus() != LetterStatus.FALLBACK_GENERATED) {
            throw new BusinessException(ErrorCode.NOTIFICATION_LETTER_NOT_READY);
        }
    }

    private void validateResultRequest(SaveNotificationResultRequest request) {
        if (request == null || request.status() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.status() != NotificationStatus.SENT
                && request.status() != NotificationStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_STATUS);
        }
    }
}
