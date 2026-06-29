package com.seenears.auth.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.OtpLog;
import com.seenears.auth.domain.OtpPurpose;
import com.seenears.auth.domain.OtpStatus;
import com.seenears.auth.domain.RefreshToken;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.dto.request.LoginOtpRequest;
import com.seenears.auth.dto.request.LoginOtpVerifyRequest;
import com.seenears.auth.dto.request.SignupOtpRequest;
import com.seenears.auth.dto.request.SignupRequest;
import com.seenears.auth.dto.request.TokenRefreshRequest;
import com.seenears.auth.dto.request.VerifySignupOtpRequest;
import com.seenears.auth.dto.response.AuthTokenResponse;
import com.seenears.auth.dto.response.OtpSendResponse;
import com.seenears.auth.dto.response.OtpVerifyResponse;
import com.seenears.auth.dto.response.TokenRefreshResponse;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.auth.repository.OtpLogRepository;
import com.seenears.auth.repository.RefreshTokenRepository;
import com.seenears.auth.sms.SmsSender;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.global.security.jwt.JwtProperties;
import com.seenears.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int OTP_EXPIRES_IN_SECONDS = 180;
    private static final int OTP_BOUND = 1_000_000;
    private static final int OTP_MAX_REQUESTS_PER_MINUTE = 3;
    private static final int OTP_MAX_REQUESTS_PER_DAY = 10;

    private final AppUserRepository appUserRepository;
    private final OtpLogRepository otpLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpLogStatusService otpLogStatusService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AppUserRepository appUserRepository,
            OtpLogRepository otpLogRepository,
            RefreshTokenRepository refreshTokenRepository,
            OtpLogStatusService otpLogStatusService,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            SmsSender smsSender
    ) {
        this.appUserRepository = appUserRepository;
        this.otpLogRepository = otpLogRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpLogStatusService = otpLogStatusService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.smsSender = smsSender;
    }

    @Transactional
    public OtpSendResponse sendSignupOtp(SignupOtpRequest request) {
        if (appUserRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        validateOtpRateLimit(request.phoneNumber(), OtpPurpose.SIGNUP, now);

        otpLogRepository.expirePendingByPhoneNumberAndPurpose(
                request.phoneNumber(),
                OtpPurpose.SIGNUP,
                OtpStatus.PENDING,
                OtpStatus.EXPIRED
        );

        String otpCode = generateOtpCode();
        OtpLog otpLog = new OtpLog(
                request.phoneNumber(),
                otpCode,
                OtpPurpose.SIGNUP,
                OtpStatus.PENDING,
                now.plusSeconds(OTP_EXPIRES_IN_SECONDS)
        );

        otpLogRepository.save(otpLog);
        smsSender.sendOtp(request.phoneNumber(), otpCode);

        return new OtpSendResponse(request.phoneNumber(), OTP_EXPIRES_IN_SECONDS);
    }

    @Transactional
    public OtpSendResponse sendLoginOtp(LoginOtpRequest request) {
        AppUser appUser = appUserRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateLoginUserStatus(appUser);

        LocalDateTime now = LocalDateTime.now();
        validateOtpRateLimit(request.phoneNumber(), OtpPurpose.LOGIN, now);

        otpLogRepository.expirePendingByPhoneNumberAndPurpose(
                request.phoneNumber(),
                OtpPurpose.LOGIN,
                OtpStatus.PENDING,
                OtpStatus.EXPIRED
        );

        String otpCode = generateOtpCode();
        OtpLog otpLog = new OtpLog(
                request.phoneNumber(),
                otpCode,
                OtpPurpose.LOGIN,
                OtpStatus.PENDING,
                now.plusSeconds(OTP_EXPIRES_IN_SECONDS)
        );

        otpLogRepository.save(otpLog);
        smsSender.sendOtp(request.phoneNumber(), otpCode);

        return new OtpSendResponse(request.phoneNumber(), OTP_EXPIRES_IN_SECONDS);
    }

    @Transactional
    public OtpVerifyResponse verifySignupOtp(VerifySignupOtpRequest request) {
        OtpLog otpLog = otpLogRepository.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                        request.phoneNumber(),
                        OtpPurpose.SIGNUP,
                        OtpStatus.PENDING
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (otpLog.getExpiresAt().isBefore(now) || otpLog.getExpiresAt().isEqual(now)) {
            otpLogStatusService.expireOtp(otpLog.getId());
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        if (!otpLog.getOtpCode().equals(request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        otpLog.markVerified(now);

        return new OtpVerifyResponse(true);
    }

    @Transactional
    public AuthTokenResponse verifyLoginOtp(LoginOtpVerifyRequest request) {
        OtpLog otpLog = otpLogRepository.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                        request.phoneNumber(),
                        OtpPurpose.LOGIN,
                        OtpStatus.PENDING
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (otpLog.getExpiresAt().isBefore(now) || otpLog.getExpiresAt().isEqual(now)) {
            otpLogStatusService.expireOtp(otpLog.getId());
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        if (!otpLog.getOtpCode().equals(request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        AppUser appUser = appUserRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateLoginUserStatus(appUser);

        otpLog.markVerified(now);
        appUser.updateLastLoginAt(now);

        return issueAuthTokens(appUser, now);
    }

    @Transactional
    public AuthTokenResponse signup(SignupRequest request) {
        if (appUserRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        OtpLog verifiedOtpLog = otpLogRepository.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                        request.phoneNumber(),
                        OtpPurpose.SIGNUP,
                        OtpStatus.VERIFIED
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_VERIFIED));

        LocalDateTime now = LocalDateTime.now();
        if (verifiedOtpLog.getExpiresAt().isBefore(now) || verifiedOtpLog.getExpiresAt().isEqual(now)) {
            otpLogStatusService.expireOtp(verifiedOtpLog.getId());
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        AppUser appUser = new AppUser(request.name(), request.phoneNumber(), UserStatus.ACTIVE);
        appUser.updateLastLoginAt(now);
        AppUser savedAppUser = appUserRepository.save(appUser);

        return issueAuthTokens(savedAppUser, now);
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String currentRefreshToken = request.refreshToken().trim();
        String subject = extractRefreshTokenSubject(currentRefreshToken);
        Long userId = parseUserId(subject);

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(currentRefreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (savedRefreshToken.getExpiresAt().isBefore(now) || savedRefreshToken.getExpiresAt().isEqual(now)) {
            refreshTokenRepository.delete(savedRefreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateLoginUserStatus(appUser);

        if (!savedRefreshToken.getAppUser().getId().equals(appUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.delete(savedRefreshToken);
        refreshTokenRepository.flush();

        String newAccessToken = jwtTokenProvider.createAccessToken(subject);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(subject);
        refreshTokenRepository.save(new RefreshToken(
                appUser,
                newRefreshToken,
                now.plus(jwtProperties.refreshTokenExpiration())
        ));

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(TokenRefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String currentRefreshToken = request.refreshToken().trim();
        String subject = extractRefreshTokenSubject(currentRefreshToken);
        Long userId = parseUserId(subject);

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(currentRefreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!savedRefreshToken.getAppUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.delete(savedRefreshToken);
    }

    private AuthTokenResponse issueAuthTokens(AppUser appUser, LocalDateTime now) {
        String subject = String.valueOf(appUser.getId());
        String accessToken = jwtTokenProvider.createAccessToken(subject);
        String refreshToken = jwtTokenProvider.createRefreshToken(subject);
        refreshTokenRepository.save(new RefreshToken(
                appUser,
                refreshToken,
                now.plus(jwtProperties.refreshTokenExpiration())
        ));

        return new AuthTokenResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getPhoneNumber(),
                accessToken,
                refreshToken
        );
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }

    private void validateOtpRateLimit(String phoneNumber, OtpPurpose purpose, LocalDateTime now) {
        long requestsInLastMinute = otpLogRepository.countByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber,
                purpose,
                now.minusMinutes(1)
        );
        if (requestsInLastMinute >= OTP_MAX_REQUESTS_PER_MINUTE) {
            throw new BusinessException(ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }

        long requestsInLastDay = otpLogRepository.countByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber,
                purpose,
                now.minusDays(1)
        );
        if (requestsInLastDay >= OTP_MAX_REQUESTS_PER_DAY) {
            throw new BusinessException(ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }
    }

    private String extractRefreshTokenSubject(String refreshToken) {
        try {
            return jwtTokenProvider.getRefreshTokenSubject(refreshToken);
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void validateLoginUserStatus(AppUser appUser) {
        if (appUser.getStatus() == UserStatus.WITHDRAW_REQUESTED) {
            throw new BusinessException(ErrorCode.USER_WITHDRAW_REQUESTED);
        }
        if (appUser.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
