package com.seenears.auth.service;

import com.seenears.auth.domain.OtpLog;
import com.seenears.auth.domain.OtpPurpose;
import com.seenears.auth.domain.OtpStatus;
import com.seenears.auth.dto.request.SignupOtpRequest;
import com.seenears.auth.dto.request.VerifySignupOtpRequest;
import com.seenears.auth.dto.response.OtpSendResponse;
import com.seenears.auth.dto.response.OtpVerifyResponse;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.auth.repository.OtpLogRepository;
import com.seenears.auth.sms.SmsSender;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int OTP_EXPIRES_IN_SECONDS = 180;
    private static final int OTP_BOUND = 1_000_000;
    private static final int SIGNUP_OTP_MAX_REQUESTS_PER_MINUTE = 3;
    private static final int SIGNUP_OTP_MAX_REQUESTS_PER_DAY = 10;

    private final AppUserRepository appUserRepository;
    private final OtpLogRepository otpLogRepository;
    private final OtpLogStatusService otpLogStatusService;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AppUserRepository appUserRepository,
            OtpLogRepository otpLogRepository,
            OtpLogStatusService otpLogStatusService,
            SmsSender smsSender
    ) {
        this.appUserRepository = appUserRepository;
        this.otpLogRepository = otpLogRepository;
        this.otpLogStatusService = otpLogStatusService;
        this.smsSender = smsSender;
    }

    @Transactional
    public OtpSendResponse sendSignupOtp(SignupOtpRequest request) {
        if (appUserRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        validateSignupOtpRateLimit(request.phoneNumber(), now);

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

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }

    private void validateSignupOtpRateLimit(String phoneNumber, LocalDateTime now) {
        long requestsInLastMinute = otpLogRepository.countByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber,
                OtpPurpose.SIGNUP,
                now.minusMinutes(1)
        );
        if (requestsInLastMinute >= SIGNUP_OTP_MAX_REQUESTS_PER_MINUTE) {
            throw new BusinessException(ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }

        long requestsInLastDay = otpLogRepository.countByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber,
                OtpPurpose.SIGNUP,
                now.minusDays(1)
        );
        if (requestsInLastDay >= SIGNUP_OTP_MAX_REQUESTS_PER_DAY) {
            throw new BusinessException(ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }
    }
}
