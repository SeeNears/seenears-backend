package com.seenears.push.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.push.domain.PushDeviceToken;
import com.seenears.push.dto.request.RegisterPushDeviceTokenRequest;
import com.seenears.push.dto.response.RegisterPushDeviceTokenResponse;
import com.seenears.push.repository.PushDeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushDeviceTokensService {

    private final AppUserRepository appUserRepository;
    private final PushDeviceTokenRepository pushDeviceTokenRepository;

    public PushDeviceTokensService(
            AppUserRepository appUserRepository,
            PushDeviceTokenRepository pushDeviceTokenRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
    }

    @Transactional
    public RegisterPushDeviceTokenResponse registerDeviceToken(
            String authenticatedUserId,
            RegisterPushDeviceTokenRequest request
    ) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        PushDeviceToken pushDeviceToken = pushDeviceTokenRepository.findByDeviceToken(request.deviceToken())
                .map(existingToken -> {
                    existingToken.registerTo(appUser, request.deviceType());
                    return existingToken;
                })
                .orElseGet(() -> new PushDeviceToken(appUser, request.deviceToken(), request.deviceType()));

        PushDeviceToken savedToken = pushDeviceTokenRepository.saveAndFlush(pushDeviceToken);

        return RegisterPushDeviceTokenResponse.from(savedToken);
    }

    private AppUser getAuthenticatedUser(String authenticatedUserId) {
        Long userId = parseUserId(authenticatedUserId);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (appUser.getStatus() == UserStatus.WITHDRAW_REQUESTED) {
            throw new BusinessException(ErrorCode.USER_WITHDRAW_REQUESTED);
        }

        if (appUser.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return appUser;
    }

    private Long parseUserId(String authenticatedUserId) {
        try {
            return Long.valueOf(authenticatedUserId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
