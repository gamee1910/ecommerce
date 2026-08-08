package io.game.service.orders.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamee1910.error.exception.ServiceException;
import io.game.service.orders.controller.client.ProductClient;
import io.game.service.orders.controller.dto.OrderRequest;
import io.game.service.orders.controller.dto.OrderResponse;
import io.game.service.orders.exception.OrderErrorCode;
import io.game.service.orders.model.Order;
import io.game.service.orders.model.OrderItem;
import io.game.service.orders.model.OrderStatus;
import io.game.service.orders.model.OutboxEvent;
import io.game.service.orders.repository.OrderRepository;
import io.game.service.orders.repository.OutboxRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "OrderService")
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse.OrderDetails createOrder(OrderRequest.Create request) {
        UUID userId = getCurrentUserId();
        String userEmail = getCurrentUserEmail();
        log.info(
                "Creating order for user={} with {} items",
                userId,
                request.items().size());

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            Map<String, Object> product = productClient.getProduct(itemReq.productId());

            if ("UNAVAILABLE".equals(product.get("status"))) {
                throw new ServiceException(OrderErrorCode.PRODUCT_UNAVAILABLE);
            }

            BigDecimal unitPrice = new BigDecimal(product.get("price").toString());
            String productName = (String) product.get("name");
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.productId())
                    .productName(productName)
                    .unitPrice(unitPrice)
                    .quantity(itemReq.quantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        log.info("Saved order id={} with status=PENDING", savedOrder.getId());

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            productClient.deductStock(itemReq.productId(), itemReq.quantity());
        }

        savedOrder.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(savedOrder);

        OutboxEvent outboxEvent = buildOutboxEvent(savedOrder, userEmail);
        outboxRepository.save(outboxEvent);
        log.info("Saved outbox event for order id={}", savedOrder.getId());

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse.OrderDetails> listMyOrders() {
        UUID userId = getCurrentUserId();
        return orderRepository.findByUserIdWithItems(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse.OrderDetails getOrderDetails(UUID id) {
        UUID userId = getCurrentUserId();
        Order order = orderRepository
                .findByIdWithItems(id)
                .orElseThrow(() -> new ServiceException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        return toResponse(order);
    }

    @Transactional
    public void cancelOrder(UUID id) {
        UUID userId = getCurrentUserId();
        Order order = orderRepository
                .findByIdWithItems(id)
                .orElseThrow(() -> new ServiceException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ServiceException(OrderErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        for (OrderItem item : order.getItems()) {
            productClient.restoreStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} cancelled by user {}", id, userId);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ServiceException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }
        return UUID.fromString(auth.getName());
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object credentials = auth.getCredentials();
        return credentials instanceof String ? (String) credentials : null;
    }

    private OutboxEvent buildOutboxEvent(Order order, String userEmail) {
        try {
            List<Map<String, Object>> items = order.getItems().stream()
                    .map(i -> Map.<String, Object>of(
                            "productId", i.getProductId().toString(),
                            "productName", i.getProductName(),
                            "quantity", i.getQuantity(),
                            "unitPrice", i.getUnitPrice(),
                            "subtotal", i.getSubtotal()))
                    .toList();

            java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", order.getId().toString());
            payload.put("userId", order.getUserId().toString());
            payload.put("totalAmount", order.getTotalAmount());
            payload.put("status", order.getStatus().name());
            payload.put("items", items);
            if (userEmail != null) {
                payload.put("userEmail", userEmail);
            }

            String payloadJson = objectMapper.writeValueAsString(payload);
            return OutboxEvent.of(order.getId(), "order.created", payloadJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for order {}", order.getId(), e);
            throw new ServiceException(OrderErrorCode.INTERNAL_ERROR);
        }
    }

    private OrderResponse.OrderDetails toResponse(Order order) {
        List<OrderResponse.OrderItemDetails> itemDetails = order.getItems().stream()
                .map(i -> new OrderResponse.OrderItemDetails(
                        i.getId(),
                        i.getProductId(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getSubtotal()))
                .toList();

        LocalDateTime createdAt =
                order.getCreatedAt() != null ? order.getCreatedAt().toLocalDateTime() : null;
        LocalDateTime updatedAt =
                order.getUpdatedAt() != null ? order.getUpdatedAt().toLocalDateTime() : null;

        return new OrderResponse.OrderDetails(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                itemDetails,
                createdAt,
                updatedAt);
    }
}
