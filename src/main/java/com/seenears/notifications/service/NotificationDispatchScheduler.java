package com.seenears.notifications.service;

import com.seenears.push.fcm.FcmPushProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.push.fcm", name = "scheduler-enabled", havingValue = "true")
public class NotificationDispatchScheduler {

    private final NotificationDispatchService notificationDispatchService;
    private final FcmPushProperties fcmPushProperties;

    public NotificationDispatchScheduler(
            NotificationDispatchService notificationDispatchService,
            FcmPushProperties fcmPushProperties
    ) {
        this.notificationDispatchService = notificationDispatchService;
        this.fcmPushProperties = fcmPushProperties;
    }

    @Scheduled(fixedDelayString = "${app.push.fcm.fixed-delay-ms:60000}")
    public void dispatchDueNotifications() {
        if (!fcmPushProperties.enabled()) {
            return;
        }

        // MVP assumes a single running backend instance; no distributed lock is used here.
        notificationDispatchService.dispatchDueNotifications();
    }
}
