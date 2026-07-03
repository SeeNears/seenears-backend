package com.seenears.notifications.repository;

import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"letter", "letter.dailyRecord"})
    Page<Notification> findByAppUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"letter", "letter.dailyRecord"})
    Optional<Notification> findByIdAndAppUserId(Long notificationId, Long userId);
}
