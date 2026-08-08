package com.ecommerce.service.notification.repository;

import com.ecommerce.service.notification.model.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventId(String eventId);
}
