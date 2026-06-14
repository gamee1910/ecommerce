package io.game.service.orders.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderRequest {

    public record Create(
            List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
            UUID productId,
            int quantity
    ) {}
}
