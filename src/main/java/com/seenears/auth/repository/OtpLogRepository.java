package com.seenears.auth.repository;

import com.seenears.auth.domain.OtpLog;
import com.seenears.auth.domain.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {

    Optional<OtpLog> findFirstByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            String phoneNumber,
            OtpPurpose purpose
    );

    long countByPhoneNumberAndPurposeAndCreatedAtAfter(
            String phoneNumber,
            OtpPurpose purpose,
            LocalDateTime createdAt
    );
}
