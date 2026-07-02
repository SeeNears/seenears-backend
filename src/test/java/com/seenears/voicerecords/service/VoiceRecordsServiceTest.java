package com.seenears.voicerecords.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.domain.MoodType;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.dto.request.CreateVoiceRecordRequest;
import com.seenears.voicerecords.dto.response.CreateVoiceRecordResponse;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoiceRecordsServiceTest {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Long USER_ID = 1L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final Long VOICE_RECORD_ID = 5L;
    private static final Long LETTER_ID = 2L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 6, 23);

    @TempDir
    private Path uploadDir;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private VoiceRecordRepository voiceRecordRepository;

    @Mock
    private LetterRepository letterRepository;

    private VoiceRecordsService voiceRecordsService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-23T09:15:00Z"), SERVICE_ZONE);
        voiceRecordsService = new VoiceRecordsService(
                appUserRepository,
                dailyRecordRepository,
                voiceRecordRepository,
                letterRepository,
                uploadDir.toString(),
                clock
        );
    }

    @Test
    void createVoiceRecordCreatesPendingLetter() {
        AppUser appUser = appUser();
        DailyRecord dailyRecord = dailyRecord(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(false);
        given(voiceRecordRepository.saveAndFlush(any(VoiceRecord.class))).willAnswer(invocation -> {
            VoiceRecord voiceRecord = invocation.getArgument(0);
            ReflectionTestUtils.setField(voiceRecord, "id", VOICE_RECORD_ID);
            ReflectionTestUtils.setField(voiceRecord, "createdAt", LocalDateTime.of(2026, 6, 23, 18, 15));
            return voiceRecord;
        });
        given(letterRepository.findByDailyRecordId(DAILY_RECORD_ID)).willReturn(Optional.empty());
        given(letterRepository.saveAndFlush(any(Letter.class))).willAnswer(invocation -> {
            Letter letter = invocation.getArgument(0);
            ReflectionTestUtils.setField(letter, "id", LETTER_ID);
            return letter;
        });

        CreateVoiceRecordResponse response = voiceRecordsService.createVoiceRecord(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID,
                new CreateVoiceRecordRequest(audioFile(), 120)
        );

        ArgumentCaptor<Letter> letterCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).saveAndFlush(letterCaptor.capture());
        Letter letter = letterCaptor.getValue();
        assertThat(letter.getDailyRecord()).isSameAs(dailyRecord);
        assertThat(letter.getAppUser()).isSameAs(appUser);
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.PENDING);
        assertThat(letter.getContent()).isNull();
        assertThat(letter.isRead()).isFalse();
        assertThat(letter.getReadAt()).isNull();
        assertThat(letter.isFallbackUsed()).isFalse();
        assertThat(letter.getGeneratedAt()).isNull();
        assertThat(letter.getLetterDate()).isEqualTo(RECORD_DATE.plusDays(1));

        assertThat(dailyRecord.getStatus()).isEqualTo(DailyRecordStatus.VOICE_SUBMITTED);
        assertThat(response.voiceRecordId()).isEqualTo(VOICE_RECORD_ID);
        assertThat(response.dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.durationSeconds()).isEqualTo(120);
        assertThat(response.sttStatus()).isEqualTo(SttStatus.PENDING);
        assertThat(response.dailyRecordStatus()).isEqualTo(DailyRecordStatus.VOICE_SUBMITTED);
        assertThat(response.letterId()).isEqualTo(LETTER_ID);
        assertThat(response.letterStatus()).isEqualTo(LetterStatus.PENDING);
        assertThat(response.letterDate()).isEqualTo(RECORD_DATE.plusDays(1));
    }

    @Test
    void createVoiceRecordDoesNotCreateDuplicateLetterWhenLetterAlreadyExists() {
        AppUser appUser = appUser();
        DailyRecord dailyRecord = dailyRecord(appUser);
        Letter existingLetter = Letter.create(dailyRecord);
        ReflectionTestUtils.setField(existingLetter, "id", LETTER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(false);
        given(voiceRecordRepository.saveAndFlush(any(VoiceRecord.class))).willAnswer(invocation -> {
            VoiceRecord voiceRecord = invocation.getArgument(0);
            ReflectionTestUtils.setField(voiceRecord, "id", VOICE_RECORD_ID);
            ReflectionTestUtils.setField(voiceRecord, "createdAt", LocalDateTime.of(2026, 6, 23, 18, 15));
            return voiceRecord;
        });
        given(letterRepository.findByDailyRecordId(DAILY_RECORD_ID)).willReturn(Optional.of(existingLetter));

        CreateVoiceRecordResponse response = voiceRecordsService.createVoiceRecord(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID,
                new CreateVoiceRecordRequest(audioFile(), 120)
        );

        verify(letterRepository, never()).saveAndFlush(any(Letter.class));
        assertThat(response.letterId()).isEqualTo(LETTER_ID);
        assertThat(response.letterStatus()).isEqualTo(LetterStatus.PENDING);
        assertThat(response.letterDate()).isEqualTo(RECORD_DATE.plusDays(1));
    }

    private MockMultipartFile audioFile() {
        return new MockMultipartFile(
                "audioFile",
                "voice.aac",
                "audio/aac",
                new byte[]{1, 2, 3}
        );
    }

    private AppUser appUser() {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", USER_ID);
        return appUser;
    }

    private DailyRecord dailyRecord(AppUser appUser) {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                RECORD_DATE,
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", DAILY_RECORD_ID);
        return dailyRecord;
    }
}
