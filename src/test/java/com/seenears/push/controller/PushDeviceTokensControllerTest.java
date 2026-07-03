package com.seenears.push.controller;

import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.push.domain.DeviceType;
import com.seenears.push.dto.request.RegisterPushDeviceTokenRequest;
import com.seenears.push.dto.response.RegisterPushDeviceTokenResponse;
import com.seenears.push.service.PushDeviceTokensService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PushDeviceTokensController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class PushDeviceTokensControllerTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PushDeviceTokensService pushDeviceTokensService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        given(jwtTokenProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(ACCESS_TOKEN))
                .willReturn(UsernamePasswordAuthenticationToken.authenticated("1", null, List.of()));
    }

    @Test
    void registerDeviceTokenReturnsSuccessResponse() throws Exception {
        RegisterPushDeviceTokenResponse response = new RegisterPushDeviceTokenResponse(
                1L,
                DeviceType.ANDROID,
                true,
                LocalDateTime.of(2026, 6, 23, 18, 0)
        );
        RegisterPushDeviceTokenRequest expectedRequest = new RegisterPushDeviceTokenRequest(
                "fcm-device-token-value",
                DeviceType.ANDROID
        );
        given(pushDeviceTokensService.registerDeviceToken(eq("1"), eq(expectedRequest)))
                .willReturn(response);

        mockMvc.perform(post("/api/push/device-tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "fcm-device-token-value",
                                  "deviceType": "ANDROID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("푸시 디바이스 토큰 등록에 성공했습니다."))
                .andExpect(jsonPath("$.data.deviceTokenId").value(1))
                .andExpect(jsonPath("$.data.deviceType").value("ANDROID"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-06-23T18:00:00"));

        verify(pushDeviceTokensService).registerDeviceToken("1", expectedRequest);
    }

    @Test
    void registerDeviceTokenFailsWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/push/device-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "fcm-device-token-value",
                                  "deviceType": "ANDROID"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(pushDeviceTokensService);
    }

    @Test
    void registerDeviceTokenFailsWhenAccessTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/push/device-tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "fcm-device-token-value",
                                  "deviceType": "ANDROID"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(pushDeviceTokensService);
    }

    @Test
    void registerDeviceTokenFailsWhenDeviceTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/push/device-tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": " ",
                                  "deviceType": "ANDROID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(pushDeviceTokensService);
    }

    @Test
    void registerDeviceTokenFailsWhenDeviceTypeIsNull() throws Exception {
        mockMvc.perform(post("/api/push/device-tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "fcm-device-token-value",
                                  "deviceType": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(pushDeviceTokensService);
    }
}
