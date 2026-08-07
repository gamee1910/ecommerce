# E-Commerce Platform

A microservices backend built with **Java 21 + Spring Boot 3.4**, covering JWT auth, two-tier caching, Kafka event-driven messaging, circuit breakers, and the Transactional Outbox Pattern.

## Services

| Service | Port | Description |
|---|---|---|
| API Gateway | 8080 | JWT validation, rate limiting, routing |
| User Service | 8081 | Auth — register, login, token rotation |
| Product Service | 8082 | Product catalog, inventory, 2-tier cache |
| Order Service | 8083 | Order lifecycle, outbox → Kafka |
| Notification Service | 8084 | Kafka consumer, email dispatch |

## Tech Stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 16 + JPA + Flyway |
| Cache | Caffeine (L1) + Redis 7 (L2) |
| Messaging | Apache Kafka 3.6 |
| Resilience | Resilience4j — Circuit Breaker, Retry |
| Auth | JJWT 0.12 — Bearer token + HttpOnly Cookie |
| Infra | Docker Compose |

## Quick Start

```bash
cp .env.example .env
make up
make kafka-create-topics

# Run each service
cd user-service         && ./mvnw spring-boot:run
cd product-service      && ./mvnw spring-boot:run
cd order-service        && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd gateway              && ./mvnw spring-boot:run
```

Kafka UI available at `http://localhost:8090`.