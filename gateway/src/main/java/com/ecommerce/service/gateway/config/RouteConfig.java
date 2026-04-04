package com.ecommerce.service.gateway.config;

import com.ecommerce.service.gateway.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RouteConfig {

  private final AuthenticationFilter authenticationFilter;

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder
        .routes()

        // Public
        .route(
            "user-service-auth",
            r ->
                r.path("/api/v1/auth/**")
                    .filters(
                        f ->
                            f.circuitBreaker(
                                c -> c.setName("userCB").setFallbackUri("forward:/fallback/user")))
                    .uri("http://localhost:8081"))

        // Protected
        .route(
            "user-service-protected",
            r ->
                r.path("/api/v1/users/**")
                    .filters(
                        f ->
                            f.filter(authenticationFilter)
                                .circuitBreaker(
                                    c ->
                                        c.setName("userCB")
                                            .setFallbackUri("forward:/fallback/user")))
                    .uri("http://localhost:8081"))
        .route(
            "product-service",
            r ->
                r.path("/api/v1/products/**")
                    .filters(
                        f ->
                            f.filter(authenticationFilter)
                                .circuitBreaker(
                                    c ->
                                        c.setName("productCB")
                                            .setFallbackUri("forward:/fallback/product")))
                    .uri("http://localhost:8082"))
        .route(
            "order-service",
            r ->
                r.path("/api/v1/orders/**")
                    .filters(
                        f ->
                            f.filter(authenticationFilter)
                                .circuitBreaker(
                                    c ->
                                        c.setName("orderCB")
                                            .setFallbackUri("forward:/fallback/order")))
                    .uri("http://localhost:8083"))
        .build();
  }
}
