package com.seenears.push.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.push.dto.request.DeactivatePushDeviceTokenRequest;
import com.seenears.push.dto.request.RegisterPushDeviceTokenRequest;
import com.seenears.push.dto.response.RegisterPushDeviceTokenResponse;
import com.seenears.push.service.PushDeviceTokensService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push/device-tokens")
public class PushDeviceTokensController {

    private static final String REGISTER_SUCCESS_MESSAGE = "푸시 디바이스 토큰 등록에 성공했습니다.";
    private static final String DEACTIVATE_SUCCESS_MESSAGE = "디바이스 토큰이 비활성화되었습니다.";

    private final PushDeviceTokensService pushDeviceTokensService;

    public PushDeviceTokensController(PushDeviceTokensService pushDeviceTokensService) {
        this.pushDeviceTokensService = pushDeviceTokensService;
    }

    @PostMapping
    public ApiResponse<RegisterPushDeviceTokenResponse> registerDeviceToken(
            Authentication authentication,
            @Valid @RequestBody RegisterPushDeviceTokenRequest request
    ) {
        RegisterPushDeviceTokenResponse response = pushDeviceTokensService.registerDeviceToken(
                authentication.getName(),
                request
        );

        return ApiResponse.success(REGISTER_SUCCESS_MESSAGE, response);
    }

    @DeleteMapping
    public ApiResponse<Void> deactivateDeviceToken(
            Authentication authentication,
            @Valid @RequestBody DeactivatePushDeviceTokenRequest request
    ) {
        pushDeviceTokensService.deactivateDeviceToken(authentication.getName(), request);

        return ApiResponse.success(DEACTIVATE_SUCCESS_MESSAGE, null);
    }
}
