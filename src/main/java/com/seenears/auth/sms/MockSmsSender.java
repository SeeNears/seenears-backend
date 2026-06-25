package com.seenears.auth.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void sendOtp(String phoneNumber, String otpCode) {
        log.info("Mock SMS signup OTP sent. phoneNumber={}, otpCode={}", phoneNumber, otpCode);
    }
}
