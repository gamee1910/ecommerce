package io.game.service.orders.order.dto;

import io.game.service.orders.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderResponse {

    public record OrderDetails(
            UUID id,
            UUID userId,
            BigDecimal totalAmount,
            OrderStatus status,
            List<OrderItemDetails> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record OrderItemDetails(
            UUID id,
            UUID productId,
            int quantity,
            BigDecimal price
    ) {}
}
