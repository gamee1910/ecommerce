# E-commerce Platform

Microservices backend platform built with Spring Boot, covering:
OAuth2, REST API, Rate Limiting, Circuit Breaker, Retry/Timeout, Bulkhead,
Caching (Redis + In-memory), Connection Pooling, Kafka, PostgreSQL.

## Architecture

```
Client → API Gateway (8080)
           ├── User Service      (8081) → users_db
           ├── Product Service   (8082) → products_db
           ├── Order Service     (8083) → orders_db
           └── Notification Svc  (8084) → notifications_db
                                     ↕
                              Kafka (9092)
                              Redis (6379)
```

## Services

| Service              | Port | Responsibility                             |
|----------------------|------|--------------------------------------------|
| api-gateway          | 8080 | Routing, Auth, Rate limit, Circuit breaker |
| user-service         | 8081 | OAuth2, JWT, User management               |
| product-service      | 8082 | Catalog, Inventory, Redis cache            |
| order-service        | 8083 | Orders, Kafka producer                     |
| notification-service | 8084 | Kafka consumer, Email/Push                 |

## Quick Start

```bash
# 1. Setup env
cp .env.example .env

# 2. Start infra
make up

# 3. Create Kafka topics
make kafka-create-topics

# 4. Verify
open http://localhost:8090   # Kafka UI
make ps                      # Check all containers
```

## Useful Commands

```bash
make logs               # Follow all logs
make kafka-topics       # List Kafka topics
make redis-cli          # Open Redis CLI
make psql-users         # Connect to users_db
make psql-products      # Connect to products_db
make psql-orders        # Connect to orders_db
make psql-notifications # Connect to notifications_db
make clean              # Remove all containers + volumes
```

## Database Schemas

Each service owns its own database (Database-per-service pattern).
Flyway migrations are located in each service under:
`src/main/resources/db/migration/`

| Database         | Service              | Key Tables                         |
|------------------|----------------------|------------------------------------|
| users_db         | user-service         | users, oauth_tokens                |
| products_db      | product-service      | products, categories               |
| orders_db        | order-service        | orders, order_items, outbox_events |
| notifications_db | notification-service | notifications                      |

## Kafka Topics

| Topic             | Producer      | Consumer             |
|-------------------|---------------|----------------------|
| order.created     | order-service | notification-service |
| order.created.DLT | Kafka (auto)  | notification-service |
| user.registered   | user-service  | notification-service |

## Weekly Progress

- [x] Week 1 — Docker Compose + DB schemas
- [ ] Week 2 — User Service (OAuth2 + JWT)
- [ ] Week 3 — API Gateway (routing + auth filter)
- [ ] Week 4 — API Gateway (rate limit + resilience)
- [ ] Week 5 — Product Service (CRUD + cache)
- [ ] Week 6 — Product Service (DB performance)
- [ ] Week 7 — Order Service + Kafka producer
- [ ] Week 8 — Notification Service (Kafka consumer)
- [ ] Week 9 — Observability (Prometheus + Grafana)
- [ ] Week 10 — Load test (k6) + final tuning
