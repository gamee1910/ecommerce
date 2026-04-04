package com.ecommerce.service.gateway.filter;

import com.ecommerce.service.gateway.config.TokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "Authentication Filter")
public class AuthenticationFilter implements GatewayFilter, Ordered {

  private final TokenVerifier tokenVerifier;

  private static final List<String> PUBLIC_PATHS = List.of("/api/v1/auth/");

  private static final String USER_ID_HEADER = "X-User-Id";
  private static final String USER_ROLE_HEADER = "X-User-Role";
  private static final String USER_EMAIL_HEADER = "X-User-Email";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    if (isPublic(path)) {
      return chain.filter(exchange);
    }

    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return unauthorized(exchange, "Missing or invalid Authorization header");
    }

    String token = authHeader.substring(7);

    try {
      Claims claims = tokenVerifier.validateAndExtract(token);

      if ("refresh".equals(claims.get("type", String.class))) {
        return unauthorized(exchange, "Refresh token not allowed");
      }

      ServerHttpRequest mutated =
          exchange
              .getRequest()
              .mutate()
              .header(USER_ID_HEADER, claims.getSubject())
              .header(USER_ROLE_HEADER, claims.get("role", String.class))
              .header(USER_EMAIL_HEADER, claims.get("email", String.class))
              .build();

      return chain.filter(exchange.mutate().request(mutated).build());

    } catch (ExpiredJwtException e) {
      log.warn("Expired JWT: {}", e.getMessage());
      return unauthorized(exchange, "Token expired");
    } catch (JwtException e) {
      log.warn("Invalid JWT: {}", e.getMessage());
      return unauthorized(exchange, "Invalid token");
    }
  }

  @Override
  public int getOrder() {
    return -100;
  }

  private boolean isPublic(String path) {
    return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    String body =
        """
                {"status": 401, "error": "%s"}
                """.formatted(message);

    DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

    return response.writeWith(Mono.just(buffer));
  }
}
