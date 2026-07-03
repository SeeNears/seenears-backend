package com.seenears.letters.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.dto.response.ReadLetterResponse;
import com.seenears.letters.dto.response.TodayLetterResponse;
import com.seenears.letters.repository.LetterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Service
public class LettersService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<LetterStatus> READABLE_STATUSES = Set.of(
            LetterStatus.GENERATED,
            LetterStatus.FALLBACK_GENERATED
    );

    private final AppUserRepository appUserRepository;
    private final LetterRepository letterRepository;
    private final Clock clock;

    @Autowired
    public LettersService(
            AppUserRepository appUserRepository,
            LetterRepository letterRepository
    ) {
        this(appUserRepository, letterRepository, Clock.system(SERVICE_ZONE));
    }

    LettersService(
            AppUserRepository appUserRepository,
            LetterRepository letterRepository,
            Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.letterRepository = letterRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TodayLetterResponse getTodayLetter(String authenticatedUserId) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        Letter letter = letterRepository.findByAppUserAndLetterDate(appUser, today)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.LETTER_NOT_FOUND,
                        "오늘 도착한 편지가 없습니다."
                ));

        return TodayLetterResponse.from(letter);
    }

    @Transactional
    public ReadLetterResponse markAsRead(String authenticatedUserId, Long letterId) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LETTER_NOT_FOUND));

        validateOwner(appUser, letter);
        validateReadableStatus(letter);

        letter.markAsRead(LocalDateTime.now(clock));

        return ReadLetterResponse.from(letter);
    }

    private void validateOwner(AppUser appUser, Letter letter) {
        if (!letter.getAppUser().getId().equals(appUser.getId())) {
            throw new BusinessException(ErrorCode.LETTER_ACCESS_DENIED);
        }
    }

    private void validateReadableStatus(Letter letter) {
        if (!READABLE_STATUSES.contains(letter.getStatus())) {
            throw new BusinessException(ErrorCode.LETTER_STATUS_NOT_ALLOWED);
        }
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
}
