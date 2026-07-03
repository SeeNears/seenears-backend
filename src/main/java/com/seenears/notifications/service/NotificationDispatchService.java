package com.seenears.notifications.service;

import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.repository.NotificationRepository;
import com.seenears.push.domain.PushDeviceToken;
import com.seenears.push.fcm.FcmNotificationSender;
import com.seenears.push.fcm.FcmPushProperties;
import com.seenears.push.fcm.FcmSendResult;
import com.seenears.push.repository.PushDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final NotificationRepository notificationRepository;
    private final PushDeviceTokenRepository pushDeviceTokenRepository;
    private final FcmNotificationSender fcmNotificationSender;
    private final FcmPushProperties fcmPushProperties;

    public NotificationDispatchService(
            NotificationRepository notificationRepository,
            PushDeviceTokenRepository pushDeviceTokenRepository,
            FcmNotificationSender fcmNotificationSender,
            FcmPushProperties fcmPushProperties
    ) {
        this.notificationRepository = notificationRepository;
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
        this.fcmNotificationSender = fcmNotificationSender;
        this.fcmPushProperties = fcmPushProperties;
    }

    @Transactional
    public int dispatchDueNotifications() {
        if (!fcmPushProperties.enabled()) {
            return 0;
        }

        List<Notification> notifications = notificationRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
                        NotificationStatus.PENDING,
                        LocalDateTime.now(SERVICE_ZONE),
                        PageRequest.of(0, fcmPushProperties.resolvedBatchSize())
                );

        int processedCount = 0;
        for (Notification notification : notifications) {
            if (dispatchOne(notification)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    private boolean dispatchOne(Notification notification) {
        try {
            List<PushDeviceToken> activeTokens = pushDeviceTokenRepository
                    .findByAppUserIdAndActiveTrueOrderByIdAsc(notification.getAppUser().getId());

            if (activeTokens.isEmpty()) {
                notification.markFailed();
                return true;
            }

            FcmSendResult sendResult = fcmNotificationSender.send(notification, activeTokens);
            deactivateInvalidTokens(sendResult.invalidTokens());

            if (sendResult.hasSuccess()) {
                notification.markSent(LocalDateTime.now(SERVICE_ZONE));
            } else {
                notification.markFailed();
            }
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to dispatch notification. notificationId={}, notificationType={}, userId={}, exceptionClass={}",
                    notification.getId(),
                    notification.getType(),
                    notification.getAppUser().getId(),
                    exception.getClass().getName()
            );
            notification.markFailed();
            return true;
        }
    }

    private void deactivateInvalidTokens(List<PushDeviceToken> invalidTokens) {
        for (PushDeviceToken invalidToken : invalidTokens) {
            invalidToken.deactivate();
        }
    }
}
