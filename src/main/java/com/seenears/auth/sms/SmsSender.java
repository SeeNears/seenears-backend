package com.seenears.auth.sms;

public interface SmsSender {

    void sendOtp(String phoneNumber, String otpCode);
}
