package com.ecommerce.service.gateway.filter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j(topic = "Coorelation Id Filter")
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        log.info(
                "Request: {} {} correlationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                finalCorrelationId);

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signalType -> log.info(
                        "Response status={} correlationId={}",
                        exchange.getResponse().getStatusCode(),
                        finalCorrelationId));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
