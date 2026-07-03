package com.seenears.push.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.push.domain.DeviceType;
import com.seenears.push.domain.PushDeviceToken;
import com.seenears.push.dto.request.RegisterPushDeviceTokenRequest;
import com.seenears.push.dto.response.RegisterPushDeviceTokenResponse;
import com.seenears.push.repository.PushDeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushDeviceTokensServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long DEVICE_TOKEN_ID = 10L;
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 6, 23, 18, 0);

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PushDeviceTokenRepository pushDeviceTokenRepository;

    private PushDeviceTokensService pushDeviceTokensService;

    @BeforeEach
    void setUp() {
        pushDeviceTokensService = new PushDeviceTokensService(appUserRepository, pushDeviceTokenRepository);
    }

    @Test
    void registerDeviceTokenCreatesNewRowWhenDeviceTokenDoesNotExist() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(pushDeviceTokenRepository.findByDeviceToken("new-token")).willReturn(Optional.empty());
        given(pushDeviceTokenRepository.saveAndFlush(any(PushDeviceToken.class))).willAnswer(invocation -> {
            PushDeviceToken pushDeviceToken = invocation.getArgument(0);
            ReflectionTestUtils.setField(pushDeviceToken, "id", DEVICE_TOKEN_ID);
            ReflectionTestUtils.setField(pushDeviceToken, "updatedAt", UPDATED_AT);
            return pushDeviceToken;
        });

        RegisterPushDeviceTokenResponse response = pushDeviceTokensService.registerDeviceToken(
                String.valueOf(USER_ID),
                new RegisterPushDeviceTokenRequest("new-token", DeviceType.ANDROID)
        );

        ArgumentCaptor<PushDeviceToken> tokenCaptor = ArgumentCaptor.forClass(PushDeviceToken.class);
        verify(pushDeviceTokenRepository).saveAndFlush(tokenCaptor.capture());
        PushDeviceToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getAppUser()).isSameAs(appUser);
        assertThat(savedToken.getDeviceToken()).isEqualTo("new-token");
        assertThat(savedToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(savedToken.isActive()).isTrue();

        assertThat(response.deviceTokenId()).isEqualTo(DEVICE_TOKEN_ID);
        assertThat(response.deviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(response.isActive()).isTrue();
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void registerDeviceTokenReactivatesAndUpdatesExistingRowWithoutCreatingNewRow() {
        AppUser appUser = appUser(USER_ID);
        PushDeviceToken existingToken = pushDeviceToken(appUser, "existing-token", DeviceType.IOS);
        ReflectionTestUtils.setField(existingToken, "id", DEVICE_TOKEN_ID);
        ReflectionTestUtils.setField(existingToken, "active", false);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(pushDeviceTokenRepository.findByDeviceToken("existing-token")).willReturn(Optional.of(existingToken));
        given(pushDeviceTokenRepository.saveAndFlush(existingToken)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(existingToken, "updatedAt", UPDATED_AT);
            return existingToken;
        });

        RegisterPushDeviceTokenResponse response = pushDeviceTokensService.registerDeviceToken(
                String.valueOf(USER_ID),
                new RegisterPushDeviceTokenRequest("existing-token", DeviceType.ANDROID)
        );

        verify(pushDeviceTokenRepository).saveAndFlush(existingToken);
        assertThat(existingToken.getAppUser()).isSameAs(appUser);
        assertThat(existingToken.getDeviceToken()).isEqualTo("existing-token");
        assertThat(existingToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(existingToken.isActive()).isTrue();
        assertThat(response.deviceTokenId()).isEqualTo(DEVICE_TOKEN_ID);
        assertThat(response.deviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void registerDeviceTokenMovesExistingTokenFromOtherUserToCurrentUser() {
        AppUser currentUser = appUser(USER_ID);
        AppUser otherUser = appUser(OTHER_USER_ID);
        PushDeviceToken existingToken = pushDeviceToken(otherUser, "shared-token", DeviceType.IOS);
        ReflectionTestUtils.setField(existingToken, "id", DEVICE_TOKEN_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(currentUser));
        given(pushDeviceTokenRepository.findByDeviceToken("shared-token")).willReturn(Optional.of(existingToken));
        given(pushDeviceTokenRepository.saveAndFlush(existingToken)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(existingToken, "updatedAt", UPDATED_AT);
            return existingToken;
        });

        pushDeviceTokensService.registerDeviceToken(
                String.valueOf(USER_ID),
                new RegisterPushDeviceTokenRequest("shared-token", DeviceType.ANDROID)
        );

        assertThat(existingToken.getAppUser()).isSameAs(currentUser);
        assertThat(existingToken.getAppUser().getId()).isEqualTo(USER_ID);
        assertThat(existingToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(existingToken.isActive()).isTrue();
    }

    @Test
    void registerDeviceTokenDoesNotDeactivateOtherActiveTokensOfSameUser() {
        AppUser appUser = appUser(USER_ID);
        PushDeviceToken otherActiveToken = pushDeviceToken(appUser, "other-token", DeviceType.IOS);
        PushDeviceToken targetToken = pushDeviceToken(appUser, "target-token", DeviceType.IOS);
        ReflectionTestUtils.setField(targetToken, "id", DEVICE_TOKEN_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(pushDeviceTokenRepository.findByDeviceToken("target-token")).willReturn(Optional.of(targetToken));
        given(pushDeviceTokenRepository.saveAndFlush(targetToken)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(targetToken, "updatedAt", UPDATED_AT);
            return targetToken;
        });

        pushDeviceTokensService.registerDeviceToken(
                String.valueOf(USER_ID),
                new RegisterPushDeviceTokenRequest("target-token", DeviceType.ANDROID)
        );

        assertThat(otherActiveToken.isActive()).isTrue();
        assertThat(otherActiveToken.getDeviceType()).isEqualTo(DeviceType.IOS);
        assertThat(targetToken.isActive()).isTrue();
        verify(pushDeviceTokenRepository, never()).saveAndFlush(otherActiveToken);
    }

    @Test
    void registerDeviceTokenThrowsUserNotFoundWhenUserDoesNotExist() {
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pushDeviceTokensService.registerDeviceToken(
                String.valueOf(USER_ID),
                new RegisterPushDeviceTokenRequest("new-token", DeviceType.ANDROID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(pushDeviceTokenRepository, never()).findByDeviceToken("new-token");
        verify(pushDeviceTokenRepository, never()).saveAndFlush(any(PushDeviceToken.class));
    }

    private AppUser appUser(Long id) {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }

    private PushDeviceToken pushDeviceToken(AppUser appUser, String deviceToken, DeviceType deviceType) {
        return new PushDeviceToken(appUser, deviceToken, deviceType);
    }
}
