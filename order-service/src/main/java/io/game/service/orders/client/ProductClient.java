package io.game.service.orders.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.game.service.orders.common.exception.OrderServiceException;
import io.game.service.orders.common.exception.OrderErrorCode;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ProductClient")
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${services.product.base-url}")
    private String productServiceUrl;

    // ────────────────────────────────────────────────────────────────────────────
    // GET product info
    // ────────────────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProduct")
    @Retry(name = "productService", fallbackMethod = "fallbackGetProduct")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProduct(UUID productId) {
        log.info("Calling product-service to get product {}", productId);
        return restTemplate.getForObject(
                productServiceUrl + "/api/v1/products/" + productId, Map.class);
    }

    public Map<String, Object> fallbackGetProduct(UUID productId, Throwable t) {
        log.error("Fallback triggered for getProduct {}: {}", productId, t.getMessage());
        throw new OrderServiceException(OrderErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Deduct stock (called during order creation)
    // ────────────────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackDeductStock")
    @Retry(name = "productService", fallbackMethod = "fallbackDeductStock")
    public void deductStock(UUID productId, int quantity) {
        log.info("Deducting {} units from product {}", quantity, productId);
        try {
            restTemplate.postForObject(
                    productServiceUrl + "/api/v1/products/" + productId + "/deduct-stock?quantity=" + quantity,
                    null,
                    Void.class);
        } catch (HttpClientErrorException.Conflict e) {
            // PRD_003 — Insufficient stock
            throw new OrderServiceException(OrderErrorCode.INSUFFICIENT_STOCK);
        }
    }

    public void fallbackDeductStock(UUID productId, int quantity, Throwable t) {
        log.error("Fallback triggered for deductStock product={} qty={}: {}", productId, quantity, t.getMessage());
        if (t instanceof OrderServiceException) {
            throw (OrderServiceException) t;
        }
        throw new OrderServiceException(OrderErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Restore stock (called during order cancellation)
    // ────────────────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackRestoreStock")
    @Retry(name = "productService", fallbackMethod = "fallbackRestoreStock")
    public void restoreStock(UUID productId, int quantity) {
        log.info("Restoring {} units to product {}", quantity, productId);
        restTemplate.postForObject(
                productServiceUrl + "/api/v1/products/" + productId + "/restore-stock?quantity=" + quantity,
                null,
                Void.class);
    }

    public void fallbackRestoreStock(UUID productId, int quantity, Throwable t) {
        // Log and swallow — best-effort restore; manual reconciliation may be needed
        log.error(
                "Fallback triggered for restoreStock product={} qty={}: {} — stock may need manual reconciliation",
                productId, quantity, t.getMessage());
    }
}
