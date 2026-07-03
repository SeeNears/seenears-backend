package com.seenears.notifications.repository;

import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findFirstByLetterIdAndTypeOrderByIdAsc(Long letterId, NotificationType type);

    List<Notification> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
            NotificationStatus status,
            LocalDateTime scheduledAt,
            Pageable pageable
    );
}
