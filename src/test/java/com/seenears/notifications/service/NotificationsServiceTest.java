package com.seenears.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.letters.domain.Letter;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
import com.seenears.notifications.dto.NotificationPageResponse;
import com.seenears.notifications.dto.NotificationResponse;
import com.seenears.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NOTIFICATION_ID = 10L;
    private static final Long LETTER_ID = 20L;
    private static final Long DAILY_RECORD_ID = 30L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 4, 8, 0);
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 7, 4, 8, 0, 1);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private NotificationsService notificationsService;

    @BeforeEach
    void setUp() {
        notificationsService = new NotificationsService(notificationRepository, appUserRepository);
    }

    @Test
    void getNotificationsReturnsOnlyAuthenticatedUsersNotifications() {
        AppUser appUser = appUser(USER_ID);
        Notification notification = letterArrivedNotification(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.findByAppUserId(eq(USER_ID), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));

        NotificationPageResponse response = notificationsService.getNotifications(String.valueOf(USER_ID), 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).notificationId()).isEqualTo(NOTIFICATION_ID);
        then(notificationRepository).should().findByAppUserId(eq(USER_ID), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void getNotificationsReturnsPagingMetadata() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.findByAppUserId(eq(USER_ID), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(new PageImpl<>(
                        List.of(reminderNotification(appUser, 11L)),
                        PageRequest.of(1, 2),
                        3
                ));

        NotificationPageResponse response = notificationsService.getNotifications(String.valueOf(USER_ID), 1, 2);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void getNotificationsSortsByScheduledAtDescAndIdDesc() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.findByAppUserId(eq(USER_ID), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        notificationsService.getNotifications(String.valueOf(USER_ID), 0, 20);

        then(notificationRepository).should().findByAppUserId(eq(USER_ID), pageableCaptor.capture());
        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("scheduledAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getNotificationReturnsDetail() {
        AppUser appUser = appUser(USER_ID);
        Notification notification = letterArrivedNotification(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.findByIdAndAppUserId(NOTIFICATION_ID, USER_ID))
                .willReturn(Optional.of(notification));

        NotificationResponse response = notificationsService.getNotification(String.valueOf(USER_ID), NOTIFICATION_ID);

        assertThat(response.notificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.type()).isEqualTo(NotificationType.LETTER_ARRIVED);
        assertThat(response.title()).isEqualTo("편지가 도착했어요");
        assertThat(response.body()).isEqualTo("오늘의 편지를 확인해보세요.");
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.letterId()).isEqualTo(LETTER_ID);
        assertThat(response.dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.scheduledAt()).isEqualTo(SCHEDULED_AT);
        assertThat(response.sentAt()).isEqualTo(SENT_AT);
    }

    @Test
    void getNotificationReturnsNullLetterAndDailyRecordIdsWhenLetterIsNull() {
        AppUser appUser = appUser(USER_ID);
        Notification notification = reminderNotification(appUser, NOTIFICATION_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(notificationRepository.findByIdAndAppUserId(NOTIFICATION_ID, USER_ID))
                .willReturn(Optional.of(notification));

        NotificationResponse response = notificationsService.getNotification(String.valueOf(USER_ID), NOTIFICATION_ID);

        assertThat(response.letterId()).isNull();
        assertThat(response.dailyRecordId()).isNull();
    }

    @Test
    void getNotificationThrowsNotFoundWhenOtherUsersNotificationIsRequested() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        given(notificationRepository.findByIdAndAppUserId(NOTIFICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationsService.getNotification(String.valueOf(USER_ID), NOTIFICATION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void getNotificationThrowsNotFoundWhenNotificationDoesNotExist() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser(USER_ID)));
        given(notificationRepository.findByIdAndAppUserId(999L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationsService.getNotification(String.valueOf(USER_ID), 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void notificationResponseDoesNotExposeReadOrAuditFields() throws JsonProcessingException {
        NotificationResponse response = NotificationResponse.from(letterArrivedNotification(appUser(USER_ID)));

        String json = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(response);

        assertThat(json).contains("\"notificationId\":10");
        assertThat(json).contains("\"letterId\":20");
        assertThat(json).contains("\"dailyRecordId\":30");
        assertThat(json).doesNotContain("isRead");
        assertThat(json).doesNotContain("readAt");
        assertThat(json).doesNotContain("read");
        assertThat(json).doesNotContain("createdAt");
        assertThat(json).doesNotContain("updatedAt");
    }

    private Notification letterArrivedNotification(AppUser appUser) {
        Notification notification = Notification.create(
                appUser,
                letter(appUser),
                NotificationType.LETTER_ARRIVED,
                "편지가 도착했어요",
                "오늘의 편지를 확인해보세요.",
                SCHEDULED_AT
        );
        notification.markSent(SENT_AT);
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        return notification;
    }

    private Notification reminderNotification(AppUser appUser, Long id) {
        Notification notification = Notification.create(
                appUser,
                null,
                NotificationType.RECORD_REMINDER,
                "기록 시간이에요",
                "오늘의 마음을 남겨주세요.",
                SCHEDULED_AT
        );
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    private Letter letter(AppUser appUser) {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                LocalDate.of(2026, 7, 3),
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
        AppUser appUser = new AppUser("테스터", "0100000000" + id, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }
}
