package com.seenears.letters.repository;

import com.seenears.auth.domain.AppUser;
import com.seenears.letters.domain.Letter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    Optional<Letter> findByDailyRecordId(Long dailyRecordId);

    Optional<Letter> findByAppUserAndLetterDate(AppUser appUser, LocalDate letterDate);

    List<Letter> findByDailyRecordIdIn(Collection<Long> dailyRecordIds);
}
