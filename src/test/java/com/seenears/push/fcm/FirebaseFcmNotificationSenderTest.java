package com.seenears.push.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationType;
import com.seenears.push.domain.DeviceType;
import com.seenears.push.domain.PushDeviceToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FirebaseFcmNotificationSenderTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 10L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 6, 23, 18, 30);

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FirebaseFcmNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new FirebaseFcmNotificationSender(firebaseMessaging, new FcmDataPayloadFactory());
    }

    @Test
    void sendReturnsAllSuccessWhenAllTokensAreSent() throws FirebaseMessagingException {
        PushDeviceToken firstToken = pushDeviceToken("token-1");
        PushDeviceToken secondToken = pushDeviceToken("token-2");
        given(firebaseMessaging.send(any(Message.class)))
                .willReturn("message-id-1", "message-id-2");

        FcmSendResult result = sender.send(notification(), List.of(firstToken, secondToken));

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    void sendTreatsUnregisteredAsInvalidToken() throws FirebaseMessagingException {
        PushDeviceToken token = pushDeviceToken("unregistered-token");
        FirebaseMessagingException exception = firebaseMessagingException(MessagingErrorCode.UNREGISTERED);
        given(firebaseMessaging.send(any(Message.class)))
                .willThrow(exception);

        FcmSendResult result = sender.send(notification(), List.of(token));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly(token);
    }

    @Test
    void sendTreatsInvalidArgumentAsInvalidToken() throws FirebaseMessagingException {
        PushDeviceToken token = pushDeviceToken("invalid-argument-token");
        FirebaseMessagingException exception = firebaseMessagingException(MessagingErrorCode.INVALID_ARGUMENT);
        given(firebaseMessaging.send(any(Message.class)))
                .willThrow(exception);

        FcmSendResult result = sender.send(notification(), List.of(token));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly(token);
    }

    @Test
    void sendDoesNotTreatOtherFirebaseMessagingExceptionAsInvalidToken() throws FirebaseMessagingException {
        PushDeviceToken token = pushDeviceToken("internal-error-token");
        FirebaseMessagingException exception = firebaseMessagingException(MessagingErrorCode.INTERNAL);
        given(firebaseMessaging.send(any(Message.class)))
                .willThrow(exception);

        FcmSendResult result = sender.send(notification(), List.of(token));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    void sendDoesNotTreatRuntimeExceptionAsInvalidToken() throws FirebaseMessagingException {
        PushDeviceToken token = pushDeviceToken("runtime-error-token");
        given(firebaseMessaging.send(any(Message.class)))
                .willThrow(new IllegalStateException("send failed"));

        FcmSendResult result = sender.send(notification(), List.of(token));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    void sendReturnsPartialSuccessAndOnlyInvalidToken() throws FirebaseMessagingException {
        PushDeviceToken validToken = pushDeviceToken("valid-token");
        PushDeviceToken invalidToken = pushDeviceToken("invalid-token");
        FirebaseMessagingException exception = firebaseMessagingException(MessagingErrorCode.UNREGISTERED);
        given(firebaseMessaging.send(any(Message.class)))
                .willReturn("message-id")
                .willThrow(exception);

        FcmSendResult result = sender.send(notification(), List.of(validToken, invalidToken));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly(invalidToken);
    }

    private FirebaseMessagingException firebaseMessagingException(MessagingErrorCode messagingErrorCode) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(messagingErrorCode);
        return exception;
    }

    private Notification notification() {
        Notification notification = Notification.create(
                appUser(),
                null,
                NotificationType.RECORD_REMINDER,
                "제목",
                "내용",
                SCHEDULED_AT
        );
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        return notification;
    }

    private PushDeviceToken pushDeviceToken(String deviceToken) {
        return new PushDeviceToken(appUser(), deviceToken, DeviceType.ANDROID);
    }

    private AppUser appUser() {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", USER_ID);
        return appUser;
    }
}
