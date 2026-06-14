package io.game.service.orders.order;

import io.game.service.orders.order.dto.OrderRequest;
import io.game.service.orders.order.dto.OrderResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse.OrderDetails> createOrder(@RequestBody OrderRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse.OrderDetails>> listMyOrders() {
        return ResponseEntity.ok(orderService.listMyOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse.OrderDetails> getOrderDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderDetails(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
