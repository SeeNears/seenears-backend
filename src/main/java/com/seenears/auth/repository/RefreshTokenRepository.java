package com.seenears.auth.repository;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByAppUser(AppUser appUser);
}
