package com.seenears.push.fcm;

import com.seenears.notifications.domain.Notification;
import com.seenears.push.domain.PushDeviceToken;

import java.util.List;

public interface FcmNotificationSender {

    FcmSendResult send(Notification notification, List<PushDeviceToken> tokens);
}
