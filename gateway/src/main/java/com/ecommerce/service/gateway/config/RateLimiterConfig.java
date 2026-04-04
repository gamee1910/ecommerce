package com.ecommerce.service.gateway.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

  // Rate limit theo userId hoặc IP
  @Bean
  public KeyResolver userKeyResolver() {
    return exchange -> {
      String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

      if (userId != null) {
        return Mono.just("user: " + userId);
      }

      // Fallback: rate limit theo IP
      InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

      String ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";

      return Mono.just("ip: " + ip);
    };
  }

  @Bean
  public RedisRateLimiter redisRateLimiter() {
    // replenishRate: token/s burstCapacity, max spike
    return new RedisRateLimiter(50, 100, 1);
  }
}
