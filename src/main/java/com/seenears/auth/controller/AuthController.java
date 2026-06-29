package com.seenears.auth.controller;

import com.seenears.auth.dto.request.LoginOtpRequest;
import com.seenears.auth.dto.request.LoginOtpVerifyRequest;
import com.seenears.auth.dto.request.SignupRequest;
import com.seenears.auth.dto.request.SignupOtpRequest;
import com.seenears.auth.dto.request.TokenRefreshRequest;
import com.seenears.auth.dto.request.VerifySignupOtpRequest;
import com.seenears.auth.dto.response.AuthTokenResponse;
import com.seenears.auth.dto.response.OtpSendResponse;
import com.seenears.auth.dto.response.OtpVerifyResponse;
import com.seenears.auth.dto.response.TokenRefreshResponse;
import com.seenears.auth.service.AuthService;
import com.seenears.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup/otp")
    public ApiResponse<OtpSendResponse> sendSignupOtp(@Valid @RequestBody SignupOtpRequest request) {
        return ApiResponse.success("인증번호가 발송되었습니다.", authService.sendSignupOtp(request));
    }

    @PostMapping("/signup/otp/verify")
    public ApiResponse<OtpVerifyResponse> verifySignupOtp(@Valid @RequestBody VerifySignupOtpRequest request) {
        return ApiResponse.success("휴대폰 인증이 완료되었습니다.", authService.verifySignupOtp(request));
    }

    @PostMapping("/login/otp")
    public ApiResponse<OtpSendResponse> sendLoginOtp(@Valid @RequestBody LoginOtpRequest request) {
        return ApiResponse.success("로그인 인증번호가 발송되었습니다.", authService.sendLoginOtp(request));
    }

    @PostMapping("/login/otp/verify")
    public ApiResponse<AuthTokenResponse> verifyLoginOtp(@Valid @RequestBody LoginOtpVerifyRequest request) {
        return ApiResponse.success("로그인이 완료되었습니다.", authService.verifyLoginOtp(request));
    }

    @PostMapping("/signup")
    public ApiResponse<AuthTokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success("회원가입이 완료되었습니다.", authService.signup(request));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success("토큰이 재발급되었습니다.", authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request);
        return ApiResponse.success("로그아웃되었습니다.", null);
    }
}
