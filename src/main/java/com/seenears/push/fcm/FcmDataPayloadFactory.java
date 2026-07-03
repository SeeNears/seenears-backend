package com.seenears.push.fcm;

import com.seenears.letters.domain.Letter;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FcmDataPayloadFactory {

    public Map<String, String> create(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("type", notification.getType().name());

        if (notification.getType() == NotificationType.LETTER_ARRIVED) {
            Letter letter = notification.getLetter();
            if (letter != null) {
                data.put("letterId", String.valueOf(letter.getId()));
                data.put("dailyRecordId", String.valueOf(letter.getDailyRecord().getId()));
            }
        }

        return data;
    }
}
