package com.seenears.push.repository;

import com.seenears.push.domain.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {

    Optional<PushDeviceToken> findByDeviceToken(String deviceToken);

    Optional<PushDeviceToken> findByDeviceTokenAndAppUserId(String deviceToken, Long appUserId);

    List<PushDeviceToken> findByAppUserIdAndActiveTrueOrderByIdAsc(Long appUserId);
}
