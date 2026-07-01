package com.seenears.voicerecords.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.dto.request.CreateVoiceRecordRequest;
import com.seenears.voicerecords.dto.response.CreateVoiceRecordResponse;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class VoiceRecordsService {

    private static final Logger log = LoggerFactory.getLogger(VoiceRecordsService.class);

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime SUBMISSION_START_TIME = LocalTime.of(18, 0);
    private static final int MIN_DURATION_SECONDS = 1;
    private static final int MAX_DURATION_SECONDS = 300;
    private static final String VOICE_RECORD_UNIQUE_CONSTRAINT = "uk_voice_records_daily_record_id";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/aac",
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/x-m4a",
            "audio/m4a"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "aac",
            "mp3",
            "m4a",
            "mp4",
            "mpeg"
    );

    private final AppUserRepository appUserRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final VoiceRecordRepository voiceRecordRepository;
    private final Path voiceRecordDir;

    public VoiceRecordsService(
            AppUserRepository appUserRepository,
            DailyRecordRepository dailyRecordRepository,
            VoiceRecordRepository voiceRecordRepository,
            @Value("${app.upload.voice-record-dir:uploads/voice-records}") String voiceRecordDir
    ) {
        this.appUserRepository = appUserRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.voiceRecordRepository = voiceRecordRepository;
        this.voiceRecordDir = Paths.get(voiceRecordDir).normalize();
    }

    @Transactional
    public CreateVoiceRecordResponse createVoiceRecord(
            String authenticatedUserId,
            Long dailyRecordId,
            CreateVoiceRecordRequest request
    ) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        DailyRecord dailyRecord = dailyRecordRepository.findById(dailyRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_RECORD_NOT_FOUND));

        validateSubmissionTime();
        validateOwner(appUser, dailyRecord);
        validateDailyRecordStatus(dailyRecord);

        if (voiceRecordRepository.existsByDailyRecord(dailyRecord)) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_ALREADY_EXISTS);
        }

        Integer durationSeconds = request.durationSeconds();
        validateDuration(durationSeconds);

        MultipartFile audioFile = request.audioFile();
        validateAudioFile(audioFile);

        StoredVoiceFile storedVoiceFile = storeAudioFile(appUser.getId(), dailyRecord.getId(), audioFile);
        try {
            VoiceRecord voiceRecord = VoiceRecord.create(
                    dailyRecord,
                    appUser,
                    storedVoiceFile.audioUrl(),
                    durationSeconds
            );
            VoiceRecord savedVoiceRecord = voiceRecordRepository.saveAndFlush(voiceRecord);

            dailyRecord.submitVoice();
            appUser.updateRecordStreak(LocalDate.now(SERVICE_ZONE));
            dailyRecordRepository.flush();
            appUserRepository.flush();

            return CreateVoiceRecordResponse.from(savedVoiceRecord);
        } catch (DataIntegrityViolationException exception) {
            deleteStoredFile(storedVoiceFile.path());
            if (isVoiceRecordDuplicateCreation(exception)) {
                throw new BusinessException(ErrorCode.VOICE_RECORD_ALREADY_EXISTS);
            }
            throw exception;
        } catch (RuntimeException exception) {
            deleteStoredFile(storedVoiceFile.path());
            throw exception;
        }
    }

    private void validateSubmissionTime() {
        LocalTime now = LocalTime.now(SERVICE_ZONE);
        if (now.isBefore(SUBMISSION_START_TIME)) {
            throw new BusinessException(ErrorCode.DAILY_RECORD_TIME_NOT_ALLOWED);
        }
    }

    private void validateOwner(AppUser appUser, DailyRecord dailyRecord) {
        if (!dailyRecord.getAppUser().getId().equals(appUser.getId())) {
            throw new BusinessException(ErrorCode.DAILY_RECORD_ACCESS_DENIED);
        }
    }

    private void validateDailyRecordStatus(DailyRecord dailyRecord) {
        if (dailyRecord.getStatus() == DailyRecordStatus.VOICE_SUBMITTED) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_ALREADY_EXISTS);
        }

        if (dailyRecord.getStatus() != DailyRecordStatus.QUESTION_ASSIGNED) {
            throw new BusinessException(ErrorCode.DAILY_RECORD_STATUS_NOT_ALLOWED);
        }
    }

    private void validateDuration(Integer durationSeconds) {
        if (durationSeconds == null) {
            return;
        }

        if (durationSeconds < MIN_DURATION_SECONDS
                || durationSeconds > MAX_DURATION_SECONDS) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_INVALID_DURATION);
        }
    }

    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_FILE_EMPTY);
        }

        String contentType = audioFile.getContentType();
        String extension = extractExtension(audioFile.getOriginalFilename());
        boolean contentTypeAllowed = contentType != null
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean extensionAllowed = extension != null
                && ALLOWED_EXTENSIONS.contains(extension);

        if (!contentTypeAllowed && !extensionAllowed) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_FILE_TYPE_NOT_ALLOWED);
        }
    }

    private StoredVoiceFile storeAudioFile(Long userId, Long dailyRecordId, MultipartFile audioFile) {
        String extension = extractExtension(audioFile.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            extension = "aac";
        }

        Path userDirectory = voiceRecordDir.resolve(String.valueOf(userId)).normalize();
        if (!userDirectory.startsWith(voiceRecordDir)) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_FILE_SAVE_FAILED);
        }

        String filename = dailyRecordId + "_" + UUID.randomUUID() + "." + extension;
        Path targetPath = userDirectory.resolve(filename).normalize();
        if (!targetPath.startsWith(userDirectory)) {
            throw new BusinessException(ErrorCode.VOICE_RECORD_FILE_SAVE_FAILED);
        }

        boolean stored = false;
        try {
            Files.createDirectories(userDirectory);
            try (InputStream inputStream = audioFile.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                stored = true;
            }
        } catch (IOException exception) {
            if (!stored) {
                deleteStoredFile(targetPath);
            }
            throw new BusinessException(ErrorCode.VOICE_RECORD_FILE_SAVE_FAILED);
        }

        return new StoredVoiceFile(voiceRecordDir.relativize(targetPath).toString(), targetPath);
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        String normalizedFilename = Paths.get(filename).getFileName().toString();
        int dotIndex = normalizedFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalizedFilename.length() - 1) {
            return null;
        }
        return normalizedFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteStoredFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Failed to delete stored voice file. path={}", path, exception);
        }
    }

    private boolean isVoiceRecordDuplicateCreation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && isVoiceRecordUniqueConstraintMessage(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isVoiceRecordUniqueConstraintMessage(String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains(VOICE_RECORD_UNIQUE_CONSTRAINT)
                || (normalizedMessage.contains("voice_records")
                && normalizedMessage.contains("daily_record_id"));
    }

    private AppUser getAuthenticatedUser(String authenticatedUserId) {
        Long userId = parseUserId(authenticatedUserId);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (appUser.getStatus() == UserStatus.WITHDRAW_REQUESTED) {
            throw new BusinessException(ErrorCode.USER_WITHDRAW_REQUESTED);
        }

        if (appUser.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return appUser;
    }

    private Long parseUserId(String authenticatedUserId) {
        try {
            return Long.valueOf(authenticatedUserId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private record StoredVoiceFile(
            String audioUrl,
            Path path
    ) {
    }
}
