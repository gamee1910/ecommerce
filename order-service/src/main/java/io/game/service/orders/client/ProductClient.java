package io.game.service.orders.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

  private final RestTemplate restTemplate;

  @Value("${services.product.base-url}")
  private String productServiceUrl;

  @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProduct")
  @Retry(name = "productService", fallbackMethod = "fallbackGetProduct")
  public Map<String, Object> getProduct(UUID productId) {
    log.info("Calling product-service to get product {}", productId);
    return restTemplate.getForObject(productServiceUrl + "/api/v1/products/" + productId, Map.class);
  }

  public Map<String, Object> fallbackGetProduct(UUID productId, Throwable t) {
    log.error("Fallback triggered for product {}, error: {}", productId, t.getMessage());
    return Map.of(
        "id", productId,
        "name", "Unknown Product (Fallback)",
        "price", 0.0,
        "status", "UNAVAILABLE"
    );
  }
}
