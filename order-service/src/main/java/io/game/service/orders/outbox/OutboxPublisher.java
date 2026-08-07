package io.game.service.orders.outbox;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
@Slf4j(topic = "OutboxPublisher")
public class OutboxPublisher {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(STATUS_PENDING);

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate
                        .send(event.getEventType(), event.getAggregateId().toString(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event id={} type={}: {}", event.getId(), event.getEventType(), ex.getMessage());
                            } else {
                                log.info("Published outbox event id={} type={} to partition={} offset={}",
                                        event.getId(), event.getEventType(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        });

                event.setStatus(STATUS_PUBLISHED);
                event.setPublishedAt(OffsetDateTime.now());
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Error publishing outbox event id={}: {}", event.getId(), e.getMessage());
                event.setStatus(STATUS_FAILED);
                outboxRepository.save(event);
            }
        }
    }
}
