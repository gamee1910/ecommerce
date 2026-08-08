package io.game.service.orders.controller.client;

import com.gamee1910.error.exception.ServiceException;
import io.game.service.orders.exception.OrderErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProduct")
    @Retry(name = "productService", fallbackMethod = "fallbackGetProduct")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProduct(UUID productId) {
        log.info("Calling product-service to get product {}", productId);
        return restTemplate.getForObject(productServiceUrl + "/api/v1/products/" + productId, Map.class);
    }


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
            throw new ServiceException(OrderErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackRestoreStock")
    @Retry(name = "productService", fallbackMethod = "fallbackRestoreStock")
    public void restoreStock(UUID productId, int quantity) {
        log.info("Restoring {} units to product {}", quantity, productId);
        restTemplate.postForObject(
                productServiceUrl + "/api/v1/products/" + productId + "/restore-stock?quantity=" + quantity,
                null,
                Void.class);
    }
}
