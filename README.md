# E-commerce Platform

Microservices backend platform built with Java 21 + Spring Boot 3.3.
Covers OAuth2, REST API design, API versioning, rate limiting, circuit breaker,
retry/timeout, bulkhead, caching (Redis + Caffeine), connection pooling,
Kafka event-driven integration, and PostgreSQL schema design.

## Architecture

```
Client
  │
  ▼
API Gateway (8080)
  │  JWT validation · Rate limiting · Circuit breaker · Routing
  │
  ├──▶ User Service (8081)        → users_db
  ├──▶ Product Service (8082)     → products_db
  ├──▶ Order Service (8083)       → orders_db
  │
  │         Kafka (9092)
  │    order.created ──▶ Notification Service (8084) → notifications_db
  │    user.registered ─▶
  │
  └──▶ Redis (6379)
         Rate limit counters · Session cache · Product cache
```

## Services & Features

### API Gateway — port 8080
- JWT Bearer token validation (delegated to User Service)
- API versioning: `/api/v1/`, `/api/v2/`
- Rate limiting: sliding window per IP/user (Bucket4j + Redis)
- Circuit breaker: CLOSED → OPEN → HALF-OPEN (Resilience4j)
- Retry: exponential backoff 100ms / 200ms / 400ms, max 3 lần
- Timeout: TimeLimiter 2s per downstream call
- Bulkhead: thread pool isolation riêng cho từng service

### User Service — port 8081
- `POST /api/v1/auth/register` — đăng ký tài khoản, trả accessToken + set refreshToken cookie
- `POST /api/v1/auth/login` — đăng nhập, trả accessToken + set refreshToken cookie
- `POST /api/v1/auth/refresh` — rotate refreshToken, trả accessToken mới
- `POST /api/v1/auth/logout` — revoke refreshToken, clear cookie
- JWT: accessToken 15 phút (body), refreshToken 7 ngày (HttpOnly cookie)
- Token rotation: mỗi lần refresh đổi cặp token mới, token cũ revoked ngay
- Force logout: revoke toàn bộ token của user (đổi password, bị xâm phạm)
- Scheduled cleanup: xóa token hết hạn / revoked khỏi DB mỗi đêm

### Product Service — port 8082
- `GET /api/v1/products` — danh sách sản phẩm (pagination, filter, full-text search)
- `GET /api/v1/products/{id}` — chi tiết sản phẩm
- `POST /api/v1/products` — tạo sản phẩm (ADMIN)
- `PUT /api/v1/products/{id}` — cập nhật sản phẩm (ADMIN)
- `DELETE /api/v1/products/{id}` — xóa sản phẩm (ADMIN)
- `GET /api/v1/categories` — danh mục sản phẩm (hỗ trợ nested)
- 2-tier cache: Caffeine L1 (in-memory) + Redis L2, TTL 5 phút
- Cache eviction tự động khi update/delete
- HikariCP tuning: pool size, timeout, max lifetime

### Order Service — port 8083
- `POST /api/v1/orders` — tạo đơn hàng (validate stock → tạo order → publish Kafka)
- `GET /api/v1/orders` — danh sách đơn hàng của user
- `GET /api/v1/orders/{id}` — chi tiết đơn hàng
- `PATCH /api/v1/orders/{id}/cancel` — hủy đơn hàng
- Transactional outbox pattern: đảm bảo Kafka event không mất khi DB commit
- Order status: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED / CANCELLED

### Notification Service — port 8084
- Consume `order.created` → gửi email xác nhận đơn hàng
- Consume `user.registered` → gửi email chào mừng
- Idempotent consumer: check duplicate event_id, không gửi 2 lần
- Dead letter topic (DLT): retry 3 lần, fail → đẩy vào `order.created.DLT`
- Lưu notification log: status PENDING / SENT / FAILED, retry count

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL 16 (JdbcClient — không dùng JPA) |
| Migration | Flyway |
| Cache | Redis 7 + Caffeine (2-tier) |
| Messaging | Apache Kafka 3.6 |
| Resilience | Resilience4j (CB + Retry + Bulkhead + TimeLimiter) |
| Rate Limiting | Bucket4j + Redis |
| Auth | JWT (JJWT 0.12) + HttpOnly Cookie |
| Build | Maven |
| Infra | Docker Compose |

## Project Structure

```
ecommerce-platform/
├── api-gateway/
├── user-service/
│   └── src/main/java/com/ecommerce/userservice/
│       ├── auth/           AuthController, AuthService, JwtService, SecurityConfig
│       ├── user/           User, UserRepository
│       ├── token/          OAuthToken, OAuthTokenRepository
│       └── common/
│           ├── dto/        AuthRequest, AuthResponse
│           ├── exception/  ErrorCode, BaseException, ErrorResponse, GlobalExceptionHandler
│           └── util/       TimeUtils
├── product-service/
├── order-service/
├── notification-service/
├── infra/
│   └── docker/
│       └── docker-compose.yml
├── scripts/
│   └── init-db.sql
├── Makefile
├── .env.example
└── README.md
```

## Quick Start

```bash
# 1. Setup env
cp .env.example .env

# 2. Start infra (PostgreSQL, Redis, Kafka, Zookeeper, Kafka UI)
make up

# 3. Tạo Kafka topics
make kafka-create-topics

# 4. Verify
open http://localhost:8090   # Kafka UI
make ps                      # Kiểm tra containers
```

## Useful Commands

```bash
make up                  # Start toàn bộ infra
make down                # Stop containers (giữ data)
make clean               # Stop + xóa toàn bộ volumes
make logs                # Follow logs tất cả services
make ps                  # Liệt kê containers + status

make kafka-topics        # List Kafka topics
make kafka-create-topics # Tạo topics cần thiết
make redis-cli           # Mở Redis CLI

make psql-users          # Connect vào users_db
make psql-products       # Connect vào products_db
make psql-orders         # Connect vào orders_db
make psql-notifications  # Connect vào notifications_db
```

## Database Schemas

Database-per-service pattern — mỗi service sở hữu DB riêng, không share.
Flyway migrations: `src/main/resources/db/migration/`

| Database | Service | Tables |
|---|---|---|
| users_db | user-service | users, oauth_tokens |
| products_db | product-service | products, categories |
| orders_db | order-service | orders, order_items, outbox_events |
| notifications_db | notification-service | notifications |

## Kafka Topics

| Topic | Producer | Consumer | Mô tả |
|---|---|---|---|
| order.created | order-service | notification-service | Đơn hàng mới tạo |
| order.created.DLT | Kafka | notification-service | Dead letter — xử lý lỗi |
| user.registered | user-service | notification-service | User đăng ký mới |

## API Error Format

```json
{
  "code": "AUTH_001",
  "message": "Email already registered",
  "timestamp": "2026-03-21 10:30:00"
}
```

| Code | HTTP | Mô tả |
|---|---|---|
| AUTH_001 | 409 | Email đã tồn tại |
| AUTH_002 | 401 | Sai email hoặc password |
| AUTH_003 | 401 | Token không hợp lệ hoặc hết hạn |
| AUTH_004 | 403 | Tài khoản bị khóa |
| USR_001 | 404 | User không tồn tại |
| CMN_001 | 400 | Validation failed |
| CMN_999 | 500 | Internal server error |

## Testing

```bash
# Register
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","fullName":"Test User"}'

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Refresh (cần cookie — dùng Postman hoặc -b/-c flag)
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -b "refresh_token=<token>"

# Logout
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -b "refresh_token=<token>"
```

Test nhanh token expiry — đổi trong `.env`:
```
JWT_ACCESS_TOKEN_EXPIRY=10000   # 10 giây
```

[//]: # (## Weekly Progress)

[//]: # ()
[//]: # (- [x] Week 1 — Docker Compose + DB schemas + project structure)

[//]: # (- [x] Week 2 — User Service &#40;OAuth2, JWT, HttpOnly Cookie, token rotation&#41;)

[//]: # (- [ ] Week 3 — API Gateway &#40;routing + JWT filter&#41;)

[//]: # (- [ ] Week 4 — API Gateway &#40;rate limit + circuit breaker + bulkhead&#41;)

[//]: # (- [ ] Week 5 — Product Service &#40;CRUD + 2-tier cache&#41;)

[//]: # (- [ ] Week 6 — Product Service &#40;DB performance + HikariCP tuning&#41;)

[//]: # (- [ ] Week 7 — Order Service + Kafka producer + outbox pattern)

[//]: # (- [ ] Week 8 — Notification Service &#40;Kafka consumer + DLT&#41;)

[//]: # (- [ ] Week 9 — Observability &#40;Prometheus + Grafana&#41;)

[//]: # (- [ ] Week 10 — Load test &#40;k6&#41; + final tuning)
