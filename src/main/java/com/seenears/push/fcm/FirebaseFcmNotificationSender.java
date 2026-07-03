package com.seenears.push.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.seenears.notifications.domain.Notification;
import com.seenears.push.domain.PushDeviceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnBean(FirebaseMessaging.class)
@ConditionalOnProperty(prefix = "app.push.fcm", name = "enabled", havingValue = "true")
public class FirebaseFcmNotificationSender implements FcmNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFcmNotificationSender.class);

    private final FirebaseMessaging firebaseMessaging;
    private final FcmDataPayloadFactory fcmDataPayloadFactory;

    public FirebaseFcmNotificationSender(
            FirebaseMessaging firebaseMessaging,
            FcmDataPayloadFactory fcmDataPayloadFactory
    ) {
        this.firebaseMessaging = firebaseMessaging;
        this.fcmDataPayloadFactory = fcmDataPayloadFactory;
    }

    @Override
    public FcmSendResult send(Notification notification, List<PushDeviceToken> tokens) {
        int successCount = 0;
        int failureCount = 0;
        List<PushDeviceToken> invalidTokens = new ArrayList<>();

        for (PushDeviceToken token : tokens) {
            try {
                firebaseMessaging.send(buildMessage(notification, token.getDeviceToken()));
                successCount++;
            } catch (FirebaseMessagingException exception) {
                failureCount++;
                log.warn(
                        "Failed to send FCM notification. notificationId={}, notificationType={}, messagingErrorCode={}",
                        notification.getId(),
                        notification.getType(),
                        exception.getMessagingErrorCode()
                );
                if (isInvalidToken(exception)) {
                    invalidTokens.add(token);
                }
            } catch (RuntimeException exception) {
                failureCount++;
                log.warn(
                        "Unexpected FCM send failure. notificationId={}, notificationType={}, exceptionClass={}",
                        notification.getId(),
                        notification.getType(),
                        exception.getClass().getName()
                );
            }
        }

        return new FcmSendResult(successCount, failureCount, invalidTokens);
    }

    private Message buildMessage(Notification notification, String deviceToken) {
        return Message.builder()
                .setToken(deviceToken)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .build())
                .putAllData(fcmDataPayloadFactory.create(notification))
                .build();
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        return messagingErrorCode == MessagingErrorCode.UNREGISTERED
                || messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
