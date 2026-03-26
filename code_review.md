# Code Review — `user-service`

**Stack:** Spring Boot 4.x · Java 21 · PostgreSQL · JJWT 0.12.5 · Flyway · JdbcClient · Lombok

---

## 🐛 Bugs

### 1. Wrong Refresh-Token Expiry in [issueTokenPair](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthService.java#103-117) — **Critical**

**File:** [AuthService.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthService.java#L111)

```java
// ❌ Wrong — uses *access* token expiry * 48 (magic number)
.expiresAt(Instant.now().plusMillis(tokenService.getAccessTokenExpiry() * 48))
```

The [Token](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/token/Token.java#7-28) stored in the DB uses `accessTokenExpiry * 48` instead of `refreshTokenExpiry`. With the default 15-minute access token, this gives only 12 hours instead of 7 days.

```java
// ✅ Fix — expose getRefreshTokenExpiry() in TokenService and use it
.expiresAt(Instant.now().plusMillis(tokenService.getRefreshTokenExpiry()))
```

---

### 2. Typo in Record Field — `accesssTokenExpiresIn`

**File:** [AuthResponse.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/dto/response/AuthResponse.java#L4)

```java
// ❌ Triple-s typo, used in 3 places (controller + record)
public record TokenPair(String accessToken, String refreshToken, long accesssTokenExpiresIn) {}
```

Rename to `accessTokenExpiresIn`. The same typo is referenced in [AuthController.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthController.java).

---

### 3. `FORBIDDEN` Mapped to Wrong HTTP Status

**File:** [UserServiceErrorCode.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/exception/UserServiceErrorCode.java#L16)

```java
// ❌ 406 Not Acceptable is semantically wrong for authorization failures
FORBIDDEN(HttpStatus.NOT_ACCEPTABLE, "CMN_002", "Forbidden"),
```

```java
// ✅ Fix
FORBIDDEN(HttpStatus.FORBIDDEN, "CMN_002", "Forbidden"),
```

---

### 4. [logout](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthController.java#59-69) Also Clears the `/api/v1/auth/logout` Path (Missing Cookie Path)

**File:** [AuthController.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthController.java#L80-L84)

The [clearRefreshTokenCookie](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthController.java#78-86) path is set to `/api/v1/auth/refresh`. If the browser sends the cookie only to that path, the logout endpoint at `/api/v1/auth/logout` will never receive it, and [extractRefreshTokenFromCookie](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthController.java#87-95) always returns `null` — meaning the token is **never revoked** on logout.

```java
// ✅ Fix — use a common parent path /api/v1/auth for both cookie operations
String.format("%s=; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=0", ...)
// and in setRefreshCookie
String.format("%s=%s; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; ...", ...)
```

---

## ⚠️ Security Concerns

### 5. `/api/v1/users` Lookup Endpoints are Unauthenticated by Security Config

**File:** [SecurityConfig.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/security/SecurityConfig.java)

`anyRequest().authenticated()` should protect `GET /api/v1/users?email=...` and `GET /api/v1/users/{userId}`, but these endpoints expose user profiles (email, role). Consider whether other internal services or only admins should call them. If this is an internal microservice, you should add IP allowlisting or a service-to-service secret header.

### 6. `GET /api/v1/users?email=...` is an Enumeration Risk

Any authenticated user can look up **any other user by email**. The endpoint has no authorization check beyond being logged in. Add an admin-only guard:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping
public ResponseEntity<UserResponse.UserProfile> getUserByEmail(@RequestParam String email) { ... }
```

### 7. Default JWT Secret in [application.yaml](file:///home/game/projects/java/ecommerce/user-service/src/main/resources/application.yaml)

```yaml
secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production-please}
```

The fallback value will be used if `JWT_SECRET` is not set. This is a common misconfiguration accident. Consider failing fast at startup if the env var is missing:

```java
// In TokenService constructor:
if (secretKey.contains("your-256-bit-secret")) {
    throw new IllegalStateException("JWT_SECRET env variable must be set in production!");
}
```

---

## 📛 Naming / Typo Issues

| Location | Issue | Suggestion |
|---|---|---|
| Package `com.ecommerce.serivce.user` | `serivce` → `service` (all 21 files) | Fix the base package name |
| [TokeRepository](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/token/TokeRepository.java#12-64) | Missing `n` → `TokenRepository` | Rename the class and file |
| `EXSIST_BY_EMAIL_SQL` constant | `EXSIST` → `EXIST` | Rename constant |
| Variable `UUIDsubject` in [UserService](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/user/UserService.java#15-77) | Violates Java naming conventions | Rename to `userIdSubject` or `userId` |
| [handlValidation](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/exception/UserServiceExceptionHandler.java#24-34) method in [UserServiceExceptionHandler](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/exception/UserServiceExceptionHandler.java#14-42) | Missing `e` | Rename to `handleValidation` |

> [!IMPORTANT]
> The package name typo (`serivce`) permeates all 21 source files and the test. While it doesn't affect runtime behavior, it should be fixed early before the project grows. In IntelliJ: right-click package → Refactor → Rename.

---

## 🏗️ Architecture / Design Observations

### 8. `UserRequest.Update` Has No Validation Constraints

**File:** [UserRequest.java](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/dto/request/UserRequest.java)

```java
public record Update(String fullName, boolean active) {}
```

`fullName` can be blank or excessively long. Add `@NotBlank` and `@Size`.

```java
public record Update(@NotBlank @Size(max = 100) String fullName, boolean active) {}
```

### 9. `UserRepository.insert` Uses a Key Holder + Extra SELECT

```java
// After INSERT RETURNING * there's still a findById round-trip
jdbcClient.sql(INSERT_USER_SQL)...update(keyHolder);
return findById(UUID.fromString(...keyHolder.getKeys()...)).orElseThrow();
```

Since the SQL already has `RETURNING *`, you can map the row directly and avoid the extra query:

```java
return jdbcClient.sql(INSERT_USER_SQL)
    .params(...)
    .query(this::mapRow)
    .single();
```

### 10. [TokenService](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/token/TokenService.java#17-75) Exposes Only `accessTokenExpiry` (Getter)

`refreshTokenExpiry` has no getter, which is what forces the `* 48` hack in bug #1. Add `@Getter` or a dedicated `getRefreshTokenExpiry()`.

### 11. [generateAccessToken](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/token/TokenService.java#37-46) Claims Order Issue (Minor)

```java
.subject(userId.toString())
.claims(Map.of("email", email, "role", role))  // ← overwrites subject!
```

In JJWT 0.12.x, `claims(Map)` replaces the entire claims map, potentially overwriting `sub`. Use `.claim("email", email).claim("role", role)` instead, or set the subject after:

```java
Jwts.builder()
    .claim("email", email)
    .claim("role", role)
    .subject(userId.toString())   // set last so it's not overwritten
    ...
```

### 12. `SecurityConfig.filterChain` is Missing `throws Exception`

```java
public SecurityFilterChain filterChain(HttpSecurity http) {   // ❌ no throws
    return http...build();
}
```

`HttpSecurity.build()` throws a checked [Exception](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/exception/UserServiceException.java#5-20). This will fail to compile without `throws Exception`. If it currently compiles, it may be suppressed somewhere. Add the declaration:

```java
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { ... }
```

---

## 🧪 Testing

The only test is an empty context-load smoke test. There are **no unit or integration tests** for:
- [AuthService](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthService.java#24-128) (login, register, refresh, revoke logic)
- [TokenService](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/token/TokenService.java#17-75) (JWT generation/validation)
- [UserService](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/user/UserService.java#15-77) (permission checks in [update](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/user/UserController.java#27-32), [getCurrentUser](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/user/UserService.java#37-51))
- [UserRepository](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/user/UserRepository.java#14-108) (JDBC queries)
- Security filter behavior

**Recommended additions:**
- Use `@WebMvcTest` + `MockMvc` for controller/filter tests
- Use `@JdbcTest` + an embedded PostgreSQL (Testcontainers) for repository tests
- Unit-test [AuthService](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/features/auth/AuthService.java#24-128) with mocked dependencies

---

## 📦 [pom.xml](file:///home/game/projects/java/ecommerce/user-service/pom.xml) Issues

The test-scope dependencies use artifact IDs that **do not exist** in Maven Central:

```xml
<!-- ❌ These are not real Spring Boot starters -->
spring-boot-starter-actuator-test
spring-boot-starter-data-jpa-test
spring-boot-starter-flyway-test
spring-boot-starter-security-test
spring-boot-starter-validation-test
spring-boot-starter-webmvc-test
```

The correct dependencies are:
```xml
<!-- ✅ Replace with the real testing starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## ✅ What's Well Done

- **Refresh token rotation** is correctly implemented (revoke old, issue new)
- **HttpOnly + Secure + SameSite=Strict** cookie for refresh token is the right approach
- **Flyway migrations** with proper index setup and auto-update trigger for `updated_at`
- **HikariCP** configured with sensible pool settings
- **[TimeUtils](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/utils/TimeUtils.java#8-140)** is a thoughtful, well-documented utility with VN timezone awareness
- **Error code system** (`UserServiceErrorCode` + [UserServiceException](file:///home/game/projects/java/ecommerce/user-service/src/main/java/com/ecommerce/serivce/user/common/exception/UserServiceException.java#5-20)) is clean and consistent
- **`JdbcClient`** usage is modern (Spring 6.1+) and avoids ORM complexity
- **Token type claim** (`"type": "refresh"`) prevents refresh tokens from being used as access tokens
