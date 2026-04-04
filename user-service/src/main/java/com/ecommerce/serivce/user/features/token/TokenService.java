package com.ecommerce.serivce.user.features.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "JWT Service")
public class TokenService {

  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_TYPE = "type";
  private static final String TYPE_REFRESH = "refresh";

  private final SecretKey secretKey;
  private final Clock clock;

  @Getter private final long accessTokenExpiry;

  @Getter private final long refreshTokenExpiry;

  public TokenService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-expiry}") long accessTokenExpiry,
      @Value("${app.jwt.refresh-token-expiry}") long refreshTokenExpiry) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiry = accessTokenExpiry;
    this.refreshTokenExpiry = refreshTokenExpiry;
    this.clock = Clock.systemUTC();
  }

  public String generateAccessToken(UUID userId, String email, String role) {
    Instant now = clock.instant();
    return Jwts.builder()
        .subject(userId.toString())
        .claim(CLAIM_EMAIL, email)
        .claim(CLAIM_ROLE, role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(accessTokenExpiry)))
        .signWith(secretKey)
        .compact();
  }

  public String generateRefreshToken(UUID userId) {
    Instant now = clock.instant();
    return Jwts.builder()
        .subject(userId.toString())
        .claim(CLAIM_TYPE, TYPE_REFRESH)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(refreshTokenExpiry)))
        .signWith(secretKey)
        .compact();
  }

  public Claims validateAndExtract(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }

  public boolean isRefreshToken(Claims claims) {
    return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
  }

  public sealed interface ValidationResult
      permits ValidationResult.Valid, ValidationResult.Expired, ValidationResult.Invalid {
    record Valid(Claims claims) implements ValidationResult {}

    record Expired() implements ValidationResult {}

    record Invalid(String reason) implements ValidationResult {}
  }

  public ValidationResult validate(String token) {
    try {
      Claims claims = validateAndExtract(token);
      return new ValidationResult.Valid(claims);
    } catch (ExpiredJwtException e) {
      log.debug("JWT expired");
      return new ValidationResult.Expired();
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("JWT invalid: {}", e.getMessage());
      return new ValidationResult.Invalid(e.getMessage());
    }
  }
}
