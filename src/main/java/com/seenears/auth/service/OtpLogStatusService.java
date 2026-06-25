package com.seenears.auth.service;

import com.seenears.auth.domain.OtpLog;
import com.seenears.auth.repository.OtpLogRepository;
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
                .orElseThrow();
        otpLog.markExpired();
    }
}
