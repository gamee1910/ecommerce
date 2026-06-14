package io.game.service.orders.order;

import io.game.service.orders.order.dto.OrderRequest;
import io.game.service.orders.order.dto.OrderResponse;
import io.game.service.orders.client.ProductClient;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    @Transactional
    public OrderResponse.OrderDetails createOrder(OrderRequest.Create request) {
        // TODO: Implement order creation
        // 1. Validate request items
        // 2. Loop through items, call ProductClient to check price and deduct stock
        // 3. Calculate total amount
        // 4. Save Order and OrderItems to DB
        // 5. Create OutboxEvent (order.created) and save to DB
        log.info("Creating order with {} items", request.items().size());
        throw new UnsupportedOperationException("createOrder not implemented yet");
    }

    @Transactional(readOnly = true)
    public List<OrderResponse.OrderDetails> listMyOrders() {
        // TODO: Get authenticated user ID, fetch orders from repository
        throw new UnsupportedOperationException("listMyOrders not implemented yet");
    }

    @Transactional(readOnly = true)
    public OrderResponse.OrderDetails getOrderDetails(UUID id) {
        // TODO: Fetch order by ID, check if it belongs to current user
        throw new UnsupportedOperationException("getOrderDetails not implemented yet");
    }

    @Transactional
    public void cancelOrder(UUID id) {
        // TODO: Fetch order, check status
        // If PENDING/CONFIRMED -> change to CANCELLED
        // Add back stock to product-service (call API)
        throw new UnsupportedOperationException("cancelOrder not implemented yet");
    }
}
