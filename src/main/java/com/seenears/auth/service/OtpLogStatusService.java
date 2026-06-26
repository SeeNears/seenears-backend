package com.seenears.auth.service;

import com.seenears.auth.domain.OtpLog;
import com.seenears.auth.repository.OtpLogRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpLogStatusService {

    private final OtpLogRepository otpLogRepository;

    public OtpLogStatusService(OtpLogRepository otpLogRepository) {
        this.otpLogRepository = otpLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOtp(Long otpLogId) {
        OtpLog otpLog = otpLogRepository.findById(otpLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_FOUND));
        otpLog.markExpired();
    }
}
