package com.seenears.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.internal.dto.request.SaveSttResultRequest;
import com.seenears.internal.dto.response.PendingSttVoiceRecordsResponse;
import com.seenears.internal.dto.response.SaveSttResultResponse;
import com.seenears.internal.service.InternalVoiceRecordsService;
import com.seenears.voicerecords.domain.SttStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalVoiceRecordsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class InternalVoiceRecordsControllerTest {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalVoiceRecordsService internalVoiceRecordsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void internalApiFailsWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/api/internal/voice-records/pending-stt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalVoiceRecordsService);
    }

    @Test
    void internalApiFailsWhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(get("/api/internal/voice-records/pending-stt")
                        .header(INTERNAL_API_KEY_HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalVoiceRecordsService);
    }

    @Test
    void getPendingSttVoiceRecordsSucceedsWithValidApiKeyAndDefaultLimit() throws Exception {
        given(internalVoiceRecordsService.getPendingSttVoiceRecords(10))
                .willReturn(new PendingSttVoiceRecordsResponse(List.of()));

        mockMvc.perform(get("/api/internal/voice-records/pending-stt")
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voiceRecords").isArray());

        verify(internalVoiceRecordsService).getPendingSttVoiceRecords(10);
    }

    @Test
    void getPendingSttVoiceRecordsPassesLimit() throws Exception {
        given(internalVoiceRecordsService.getPendingSttVoiceRecords(5))
                .willReturn(new PendingSttVoiceRecordsResponse(List.of()));

        mockMvc.perform(get("/api/internal/voice-records/pending-stt")
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .param("limit", "5"))
                .andExpect(status().isOk());

        verify(internalVoiceRecordsService).getPendingSttVoiceRecords(5);
    }

    @Test
    void saveSttResultSucceedsWithValidApiKey() throws Exception {
        given(internalVoiceRecordsService.saveSttResult(eq(5L), any(SaveSttResultRequest.class)))
                .willReturn(new SaveSttResultResponse(5L, SttStatus.SUCCESS));

        mockMvc.perform(patch("/api/internal/voice-records/{voiceRecordId}/stt-result", 5L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveSttResultRequest(SttStatus.SUCCESS, "오늘은 산책을 다녀왔습니다.")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voiceRecordId").value(5))
                .andExpect(jsonPath("$.data.sttStatus").value("SUCCESS"));
    }

    @Test
    void saveSttResultFailsWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/internal/voice-records/{voiceRecordId}/stt-result", 5L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sttText\":\"오늘은 산책을 다녀왔습니다.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
