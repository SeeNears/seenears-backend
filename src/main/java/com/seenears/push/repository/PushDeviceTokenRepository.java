package com.seenears.push.repository;

import com.seenears.push.domain.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {

    Optional<PushDeviceToken> findByDeviceToken(String deviceToken);
}
