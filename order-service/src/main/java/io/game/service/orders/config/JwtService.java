package io.game.service.orders.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "JwtService")
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
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
