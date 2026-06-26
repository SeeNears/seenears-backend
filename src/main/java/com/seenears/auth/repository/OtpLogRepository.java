package com.seenears.auth.repository;

import com.seenears.auth.domain.OtpLog;
import com.seenears.auth.domain.OtpPurpose;
import com.seenears.auth.domain.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {

    Optional<OtpLog> findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
            String phoneNumber,
            OtpPurpose purpose,
            OtpStatus status
    );

    long countByPhoneNumberAndPurposeAndCreatedAtAfter(
            String phoneNumber,
            OtpPurpose purpose,
            LocalDateTime createdAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OtpLog o
            set o.status = :expiredStatus
            where o.phoneNumber = :phoneNumber
              and o.purpose = :purpose
              and o.status = :pendingStatus
            """)
    int expirePendingByPhoneNumberAndPurpose(
            @Param("phoneNumber") String phoneNumber,
            @Param("purpose") OtpPurpose purpose,
            @Param("pendingStatus") OtpStatus pendingStatus,
            @Param("expiredStatus") OtpStatus expiredStatus
    );
}
