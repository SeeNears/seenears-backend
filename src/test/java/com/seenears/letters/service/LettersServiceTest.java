package com.seenears.letters.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.dto.response.ReadLetterResponse;
import com.seenears.letters.repository.LetterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LettersServiceTest {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long LETTER_ID = 2L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final LocalDateTime READ_AT = LocalDateTime.of(2026, 6, 23, 19, 0);

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private LetterRepository letterRepository;

    private LettersService lettersService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-23T10:00:00Z"), SERVICE_ZONE);
        lettersService = new LettersService(appUserRepository, letterRepository, clock);
    }

    @Test
    void markAsReadSucceedsForGeneratedLetterFirstRead() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = generatedLetter(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        ReadLetterResponse response = lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID);

        assertThat(response.letterId()).isEqualTo(LETTER_ID);
        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isEqualTo(READ_AT);
        assertThat(letter.isRead()).isTrue();
        assertThat(letter.getReadAt()).isEqualTo(READ_AT);
    }

    @Test
    void markAsReadSucceedsForFallbackGeneratedLetterFirstRead() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = fallbackGeneratedLetter(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        ReadLetterResponse response = lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isEqualTo(READ_AT);
        assertThat(letter.isRead()).isTrue();
        assertThat(letter.getReadAt()).isEqualTo(READ_AT);
    }

    @Test
    void markAsReadIsIdempotentAndKeepsExistingReadAt() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = generatedLetter(appUser);
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 6, 22, 20, 0);
        letter.markAsRead(firstReadAt);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        ReadLetterResponse response = lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isEqualTo(firstReadAt);
        assertThat(letter.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    void markAsReadThrowsAccessDeniedWhenOwnerIsDifferentAndDoesNotChangeReadFields() {
        AppUser appUser = appUser(USER_ID);
        AppUser otherUser = appUser(OTHER_USER_ID);
        Letter letter = generatedLetter(otherUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        assertThatThrownBy(() -> lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_ACCESS_DENIED);
        assertThat(letter.isRead()).isFalse();
        assertThat(letter.getReadAt()).isNull();
    }

    @Test
    void markAsReadThrowsNotFoundWhenLetterDoesNotExist() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_NOT_FOUND);
    }

    @Test
    void markAsReadThrowsStatusNotAllowedForPendingLetter() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = pendingLetter(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        assertThatThrownBy(() -> lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_STATUS_NOT_ALLOWED);
        assertThat(letter.isRead()).isFalse();
        assertThat(letter.getReadAt()).isNull();
    }

    @Test
    void markAsReadThrowsStatusNotAllowedForFailedLetter() {
        AppUser appUser = appUser(USER_ID);
        Letter letter = failedLetter(appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));

        assertThatThrownBy(() -> lettersService.markAsRead(String.valueOf(USER_ID), LETTER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_STATUS_NOT_ALLOWED);
        assertThat(letter.isRead()).isFalse();
        assertThat(letter.getReadAt()).isNull();
    }

    private AppUser appUser(Long id) {
        AppUser appUser = new AppUser("테스터", "01000000000" + id, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }

    private DailyRecord dailyRecord(AppUser appUser) {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                LocalDate.of(2026, 6, 22),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", DAILY_RECORD_ID);
        return dailyRecord;
    }

    private Letter pendingLetter(AppUser appUser) {
        Letter letter = Letter.create(dailyRecord(appUser));
        ReflectionTestUtils.setField(letter, "id", LETTER_ID);
        return letter;
    }

    private Letter generatedLetter(AppUser appUser) {
        Letter letter = pendingLetter(appUser);
        letter.saveGenerated("편지 본문", false);
        return letter;
    }

    private Letter fallbackGeneratedLetter(AppUser appUser) {
        Letter letter = pendingLetter(appUser);
        letter.saveGenerated("편지 본문", true);
        return letter;
    }

    private Letter failedLetter(AppUser appUser) {
        Letter letter = pendingLetter(appUser);
        letter.saveFailed();
        return letter;
    }
}
