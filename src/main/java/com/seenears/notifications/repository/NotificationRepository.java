package com.seenears.notifications.repository;

import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findFirstByLetterIdAndTypeOrderByIdAsc(Long letterId, NotificationType type);
}
