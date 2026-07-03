package com.seenears.notifications.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
import com.seenears.letters.domain.Letter;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
import com.seenears.notifications.repository.NotificationRepository;
import com.seenears.push.domain.DeviceType;
import com.seenears.push.domain.PushDeviceToken;
import com.seenears.push.fcm.FcmDataPayloadFactory;
import com.seenears.push.fcm.FcmNotificationSender;
import com.seenears.push.fcm.FcmPushProperties;
import com.seenears.push.fcm.FcmSendResult;
import com.seenears.push.repository.PushDeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final Long LETTER_ID = 20L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 6, 23, 18, 30);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushDeviceTokenRepository pushDeviceTokenRepository;

    private FakeFcmNotificationSender fakeSender;
    private NotificationDispatchService notificationDispatchService;

    @BeforeEach
    void setUp() {
        fakeSender = new FakeFcmNotificationSender();
        notificationDispatchService = new NotificationDispatchService(
                notificationRepository,
                pushDeviceTokenRepository,
                fakeSender,
                new FcmPushProperties(true, false, 20, 60_000L, "")
        );
    }

    @Test
    void dispatchDueNotificationsMarksSentWhenFcmSendSucceeds() {
        Notification notification = notification(NotificationType.RECORD_REMINDER);
        PushDeviceToken token = pushDeviceToken("token-1");
        givenDueNotifications(notification);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of(token));
        fakeSender.nextResult = new FcmSendResult(1, 0, List.of());

        int processedCount = notificationDispatchService.dispatchDueNotifications();

        assertThat(processedCount).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    void dispatchDueNotificationsMarksFailedWhenNoActivePushTokenExists() {
        Notification notification = notification(NotificationType.RECORD_REMINDER);
        givenDueNotifications(notification);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of());

        notificationDispatchService.dispatchDueNotifications();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getSentAt()).isNull();
        assertThat(fakeSender.sentNotifications).isEmpty();
    }

    @Test
    void dispatchDueNotificationsMarksFailedWhenAllTokensFail() {
        Notification notification = notification(NotificationType.RECORD_REMINDER);
        PushDeviceToken token = pushDeviceToken("token-1");
        givenDueNotifications(notification);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of(token));
        fakeSender.nextResult = new FcmSendResult(0, 1, List.of());

        notificationDispatchService.dispatchDueNotifications();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void dispatchDueNotificationsMarksSentAndDeactivatesInvalidTokenWhenAnyTokenSucceeds() {
        Notification notification = notification(NotificationType.RECORD_REMINDER);
        PushDeviceToken validToken = pushDeviceToken("valid-token");
        PushDeviceToken invalidToken = pushDeviceToken("invalid-token");
        givenDueNotifications(notification);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of(validToken, invalidToken));
        fakeSender.nextResult = new FcmSendResult(1, 1, List.of(invalidToken));

        notificationDispatchService.dispatchDueNotifications();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(validToken.isActive()).isTrue();
        assertThat(invalidToken.isActive()).isFalse();
    }

    @Test
    void dispatchDueNotificationsBuildsLetterArrivedPayloadWithLetterIdAndDailyRecordId() {
        Notification notification = letterArrivedNotification();
        PushDeviceToken token = pushDeviceToken("token-1");
        givenDueNotifications(notification);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of(token));
        fakeSender.nextResult = new FcmSendResult(1, 0, List.of());

        notificationDispatchService.dispatchDueNotifications();

        assertThat(fakeSender.sentPayloads).singleElement()
                .satisfies(payload -> {
                    assertThat(payload).containsEntry("type", "LETTER_ARRIVED");
                    assertThat(payload).containsEntry("letterId", String.valueOf(LETTER_ID));
                    assertThat(payload).containsEntry("dailyRecordId", String.valueOf(DAILY_RECORD_ID));
                });
    }

    @Test
    void dispatchDueNotificationsBuildsReminderPayloadWithoutLetterIdAndDailyRecordId() {
        Notification recordReminder = notification(NotificationType.RECORD_REMINDER);
        Notification inactiveReminder = notification(NotificationType.THREE_DAY_INACTIVE_REMINDER);
        PushDeviceToken token = pushDeviceToken("token-1");
        givenDueNotifications(recordReminder, inactiveReminder);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of(token));
        fakeSender.nextResult = new FcmSendResult(1, 0, List.of());

        notificationDispatchService.dispatchDueNotifications();

        assertThat(fakeSender.sentPayloads).hasSize(2);
        assertThat(fakeSender.sentPayloads)
                .allSatisfy(payload -> assertThat(payload).doesNotContainKeys("letterId", "dailyRecordId"));
    }

    @Test
    void dispatchDueNotificationsContinuesWithNextNotificationWhenOneNotificationThrows() {
        Notification first = notification(NotificationType.RECORD_REMINDER);
        ReflectionTestUtils.setField(first, "id", 101L);
        Notification second = notification(NotificationType.THREE_DAY_INACTIVE_REMINDER);
        ReflectionTestUtils.setField(second, "id", 102L);
        PushDeviceToken token = pushDeviceToken("token-1");
        givenDueNotifications(first, second);
        given(pushDeviceTokenRepository.findByAppUserIdAndActiveTrueOrderByIdAsc(USER_ID))
                .willReturn(List.of(token));
        fakeSender.throwOnFirstSend = true;
        fakeSender.nextResult = new FcmSendResult(1, 0, List.of());

        int processedCount = notificationDispatchService.dispatchDueNotifications();

        assertThat(processedCount).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(second.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void dispatchDueNotificationsUsesConfiguredBatchSize() {
        NotificationDispatchService service = new NotificationDispatchService(
                notificationRepository,
                pushDeviceTokenRepository,
                fakeSender,
                new FcmPushProperties(true, false, 7, 60_000L, "")
        );
        givenDueNotifications();

        service.dispatchDueNotifications();

        verify(notificationRepository).findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.argThat((Pageable pageable) -> pageable.getPageSize() == 7)
        );
    }

    private void givenDueNotifications(Notification... notifications) {
        given(notificationRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(notifications));
    }

    private Notification notification(NotificationType type) {
        return Notification.create(
                appUser(),
                null,
                type,
                "제목",
                "내용",
                SCHEDULED_AT
        );
    }

    private Notification letterArrivedNotification() {
        return Notification.create(
                appUser(),
                generatedLetter(),
                NotificationType.LETTER_ARRIVED,
                "마음 편지가 도착했어요",
                "오늘의 기록을 바탕으로 편지가 도착했어요.",
                SCHEDULED_AT
        );
    }

    private Letter generatedLetter() {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser(),
                LocalDate.of(2026, 6, 22),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", DAILY_RECORD_ID);

        Letter letter = Letter.create(dailyRecord);
        ReflectionTestUtils.setField(letter, "id", LETTER_ID);
        letter.saveGenerated("편지 본문", false);
        return letter;
    }

    private PushDeviceToken pushDeviceToken(String deviceToken) {
        return new PushDeviceToken(appUser(), deviceToken, DeviceType.ANDROID);
    }

    private AppUser appUser() {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", USER_ID);
        return appUser;
    }

    private static class FakeFcmNotificationSender implements FcmNotificationSender {

        private final FcmDataPayloadFactory payloadFactory = new FcmDataPayloadFactory();
        private final List<Notification> sentNotifications = new ArrayList<>();
        private final List<Map<String, String>> sentPayloads = new ArrayList<>();
        private FcmSendResult nextResult = FcmSendResult.success();
        private boolean throwOnFirstSend = false;

        @Override
        public FcmSendResult send(Notification notification, List<PushDeviceToken> tokens) {
            if (throwOnFirstSend) {
                throwOnFirstSend = false;
                throw new IllegalStateException("first send failed");
            }

            sentNotifications.add(notification);
            sentPayloads.add(payloadFactory.create(notification));
            return nextResult;
        }
    }
}
