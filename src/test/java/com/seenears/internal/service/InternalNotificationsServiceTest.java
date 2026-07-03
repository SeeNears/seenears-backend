package com.seenears.internal.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalNotificationsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 9L;
    private static final Long LETTER_ID = 2L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final Long NOTIFICATION_ID = 20L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 6, 23, 18, 30);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private LetterRepository letterRepository;

    private InternalNotificationsService internalNotificationsService;

    @BeforeEach
    void setUp() {
        internalNotificationsService =
                new InternalNotificationsService(notificationRepository, appUserRepository, letterRepository);
    }

    @Test
    void createNotificationSucceedsWithLetterArrived() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = generatedLetter(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(notificationRepository.findFirstByLetterIdAndTypeOrderByIdAsc(
                LETTER_ID,
                NotificationType.LETTER_ARRIVED
        )).willReturn(Optional.empty());
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
            return notification;
        });

        CreateNotificationResponse response =
                internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, LETTER_ID));

        assertThat(response.notificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.type()).isEqualTo(NotificationType.LETTER_ARRIVED);
        assertThat(response.letterId()).isEqualTo(LETTER_ID);
        assertThat(response.dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.scheduledAt()).isEqualTo(SCHEDULED_AT);
    }

    @Test
    void createNotificationThrowsInvalidInputWhenLetterArrivedLetterIdIsMissing() {
        assertThatThrownBy(() -> internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void createNotificationThrowsNotFoundWhenUserDoesNotExist() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, LETTER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void createNotificationThrowsNotFoundWhenLetterDoesNotExist() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, LETTER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_NOT_FOUND);
    }

    @Test
    void createNotificationThrowsAccessDeniedWhenLetterUserDoesNotMatch() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(generatedLetter(appUser(OTHER_USER_ID))));

        assertThatThrownBy(() -> internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, LETTER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_ACCESS_DENIED);
    }

    @Test
    void createNotificationThrowsWhenLetterArrivedLetterIsNotGenerated() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = pendingLetter(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        assertThatThrownBy(() -> internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, LETTER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_LETTER_NOT_READY);
    }

    @Test
    void createNotificationReturnsExistingNotificationWhenLetterArrivedAlreadyExists() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = generatedLetter(appUser);
        Notification existing = notification(appUser, letter);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(notificationRepository.findFirstByLetterIdAndTypeOrderByIdAsc(
                LETTER_ID,
                NotificationType.LETTER_ARRIVED
        )).willReturn(Optional.of(existing));

        CreateNotificationResponse response =
                internalNotificationsService.createNotification(letterArrivedRequest(USER_ID, LETTER_ID));

        assertThat(response.notificationId()).isEqualTo(NOTIFICATION_ID);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void createNotificationAllowsRecordReminderWithoutLetterId() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
            return notification;
        });

        CreateNotificationResponse response = internalNotificationsService.createNotification(
                new CreateNotificationRequest(
                        USER_ID,
                        null,
                        NotificationType.RECORD_REMINDER,
                        "기록 시간이에요",
                        "오늘의 마음을 남겨주세요.",
                        SCHEDULED_AT
                )
        );

        assertThat(response.type()).isEqualTo(NotificationType.RECORD_REMINDER);
        assertThat(response.letterId()).isNull();
        assertThat(response.dailyRecordId()).isNull();
    }

    @Test
    void createNotificationAllowsThreeDayInactiveReminderWithoutLetterId() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
            return notification;
        });

        CreateNotificationResponse response = internalNotificationsService.createNotification(
                new CreateNotificationRequest(
                        USER_ID,
                        null,
                        NotificationType.THREE_DAY_INACTIVE_REMINDER,
                        "기록을 기다리고 있어요",
                        "오랜만에 오늘의 마음을 들려주세요.",
                        SCHEDULED_AT
                )
        );

        assertThat(response.type()).isEqualTo(NotificationType.THREE_DAY_INACTIVE_REMINDER);
        assertThat(response.letterId()).isNull();
    }

    @Test
    void saveNotificationResultStoresSentWithRequestedSentAt() {
        Notification notification = notification(appUser(USER_ID), null);
        LocalDateTime sentAt = LocalDateTime.of(2026, 6, 23, 18, 30, 5);
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

        SaveNotificationResultResponse response = internalNotificationsService.saveNotificationResult(
                NOTIFICATION_ID,
                new SaveNotificationResultRequest(NotificationStatus.SENT, sentAt)
        );

        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void saveNotificationResultStoresSentWithNowWhenSentAtIsMissing() {
        Notification notification = notification(appUser(USER_ID), null);
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

        internalNotificationsService.saveNotificationResult(
                NOTIFICATION_ID,
                new SaveNotificationResultRequest(NotificationStatus.SENT, null)
        );

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    void saveNotificationResultStoresFailed() {
        Notification notification = notification(appUser(USER_ID), null);
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

        SaveNotificationResultResponse response = internalNotificationsService.saveNotificationResult(
                NOTIFICATION_ID,
                new SaveNotificationResultRequest(NotificationStatus.FAILED, null)
        );

        assertThat(response.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void saveNotificationResultThrowsNotFoundWhenNotificationDoesNotExist() {
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalNotificationsService.saveNotificationResult(
                NOTIFICATION_ID,
                new SaveNotificationResultRequest(NotificationStatus.SENT, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void saveNotificationResultThrowsBadRequestWhenStatusIsPending() {
        assertThatThrownBy(() -> internalNotificationsService.saveNotificationResult(
                NOTIFICATION_ID,
                new SaveNotificationResultRequest(NotificationStatus.PENDING, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_NOTIFICATION_STATUS);
    }

    private CreateNotificationRequest letterArrivedRequest(Long userId, Long letterId) {
        return new CreateNotificationRequest(
                userId,
                letterId,
                NotificationType.LETTER_ARRIVED,
                "마음 편지가 도착했어요",
                "오늘의 기록을 바탕으로 편지가 도착했어요.",
                SCHEDULED_AT
        );
    }

    private Notification notification(AppUser appUser, Letter letter) {
        Notification notification = Notification.create(
                appUser,
                letter,
                letter == null ? NotificationType.RECORD_REMINDER : NotificationType.LETTER_ARRIVED,
                "제목",
                "내용",
                SCHEDULED_AT
        );
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        return notification;
    }

    private Letter generatedLetter(AppUser appUser) {
        Letter letter = pendingLetter(appUser);
        letter.saveGenerated("편지 본문", false);
        return letter;
    }

    private Letter pendingLetter(AppUser appUser) {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                LocalDate.of(2026, 6, 22),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", DAILY_RECORD_ID);

        Letter letter = Letter.create(dailyRecord);
        ReflectionTestUtils.setField(letter, "id", LETTER_ID);
        return letter;
    }

    private AppUser appUser(Long id) {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }
}
