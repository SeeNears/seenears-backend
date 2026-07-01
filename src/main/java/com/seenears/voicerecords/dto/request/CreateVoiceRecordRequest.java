package com.seenears.voicerecords.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record CreateVoiceRecordRequest(
        MultipartFile audioFile,
        Integer durationSeconds
) {
}
