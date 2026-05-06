package io.game.service.orders.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID aggregateId;

  @Column(nullable = false, length = 100)
  private String eventType;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  private OffsetDateTime publishedAt;

  @PrePersist
  void onCreate() {
    createdAt = OffsetDateTime.now();
  }

  public static OutboxEvent of(UUID orderId, String eventType, String payload) {
    OutboxEvent e = new OutboxEvent();
    e.aggregateId = orderId;
    e.eventType = eventType;
    e.payload = payload;
    return e;
  }
}
