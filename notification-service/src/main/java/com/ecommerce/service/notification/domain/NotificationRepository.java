package com.ecommerce.service.notification.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventId(String eventId);

    Optional<Notification> findByEventId(String eventId);
}
