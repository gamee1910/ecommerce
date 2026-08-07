package com.ecommerce.service.notification.consumer;

import com.ecommerce.service.notification.domain.Notification;
import com.ecommerce.service.notification.domain.NotificationRepository;
import com.ecommerce.service.notification.domain.NotificationStatus;
import com.ecommerce.service.notification.domain.NotificationType;
import com.ecommerce.service.notification.email.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "UserRegisteredConsumer")
public class UserRegisteredConsumer {

    private static final String TOPIC = "user.registered";

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "false")
    @KafkaListener(topics = TOPIC, groupId = "notification-service")
    @Transactional
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = buildEventId(record);
        log.info("Received user.registered event — key={} partition={} offset={}", record.key(), record.partition(), record.offset());

        // Idempotency check
        if (notificationRepository.existsByEventId(eventId)) {
            log.warn("Duplicate user.registered event detected, skipping. eventId={}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(record.value());
            String email = payload.get("email").asText();
            String userId = payload.get("userId").asText();

            // Build notification record
            Notification notification = Notification.builder()
                    .eventId(eventId)
                    .type(NotificationType.USER_REGISTERED)
                    .recipientEmail(email)
                    .subject("Chào mừng bạn đến với E-Commerce Platform!")
                    .status(NotificationStatus.PENDING)
                    .build();

            notificationRepository.save(notification);

            // Send email
            emailService.sendWelcomeEmail(email);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);

            log.info("Welcome email sent to={} userId={}", email, userId);

        } catch (Exception e) {
            log.error("Failed to process user.registered event eventId={}: {}", eventId, e.getMessage(), e);
            throw new RuntimeException("Failed to process user.registered: " + e.getMessage(), e);
        }

        ack.acknowledge();
    }

    private String buildEventId(ConsumerRecord<String, String> record) {
        return TOPIC + ":" + record.partition() + ":" + record.offset();
    }
}
