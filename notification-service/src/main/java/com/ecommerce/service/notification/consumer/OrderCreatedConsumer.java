package com.ecommerce.service.notification.consumer;

import com.ecommerce.service.notification.model.Notification;
import com.ecommerce.service.notification.model.NotificationStatus;
import com.ecommerce.service.notification.model.NotificationType;
import com.ecommerce.service.notification.repository.NotificationRepository;
import com.ecommerce.service.notification.service.EmailService;
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
@Slf4j(topic = "OrderCreatedConsumer")
public class OrderCreatedConsumer {

    private static final String TOPIC = "order.created";

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "false")
    @KafkaListener(topics = TOPIC, groupId = "notification-service")
    @Transactional
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = buildEventId(record);
        log.info(
                "Received order.created event — key={} partition={} offset={}",
                record.key(),
                record.partition(),
                record.offset());

        // Idempotency check
        if (notificationRepository.existsByEventId(eventId)) {
            log.warn("Duplicate order.created event detected, skipping. eventId={}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(record.value());
            String orderId = payload.get("orderId").asText();
            String totalAmount = payload.get("totalAmount").asText();

            String userEmail;
            if (payload.has("userEmail")) {
                userEmail = payload.get("userEmail").asText();
            } else {
                log.warn(
                        "order.created payload missing userEmail for orderId={}, cannot send confirmation email",
                        orderId);
                ack.acknowledge();
                return;
            }

            String subject = String.format(
                    "Xác nhận đơn hàng #%s thành công", orderId.substring(0, Math.min(8, orderId.length())));

            Notification notification = Notification.builder()
                    .eventId(eventId)
                    .type(NotificationType.ORDER_CREATED)
                    .recipientEmail(userEmail)
                    .subject(subject)
                    .status(NotificationStatus.PENDING)
                    .build();

            notificationRepository.save(notification);

            emailService.sendOrderConfirmationEmail(userEmail, orderId, totalAmount);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);

            log.info("Order confirmation email sent to={} orderId={}", userEmail, orderId);

        } catch (Exception e) {
            log.error("Failed to process order.created event eventId={}: {}", eventId, e.getMessage(), e);
            throw new RuntimeException("Failed to process order.created: " + e.getMessage(), e);
        }

        ack.acknowledge();
    }

    private String buildEventId(ConsumerRecord<String, String> record) {
        return TOPIC + ":" + record.partition() + ":" + record.offset();
    }
}
