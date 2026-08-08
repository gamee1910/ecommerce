package com.ecommerce.serivce.users.service;

import com.ecommerce.serivce.users.controller.dto.AuthRequest;
import com.ecommerce.serivce.users.controller.dto.AuthResponse;
import com.ecommerce.serivce.users.exception.UserServiceErrorCode;
import com.ecommerce.serivce.users.model.Token;
import com.ecommerce.serivce.users.model.User;
import com.ecommerce.serivce.users.repository.TokenRepository;
import com.ecommerce.serivce.users.repository.UserRepository;
import com.gamee1910.error.exception.ServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Auth Service")
public class AuthService {

    private static final String TOPIC_USER_REGISTERED = "user.registered";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public AuthResponse.TokenPair register(AuthRequest.Register request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ServiceException(UserServiceErrorCode.EMAIL_ALREADY_EXIST);
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .build());

        log.info("Registered new user: {}", user.getId());

        try {
            kafkaTemplate.send(
                    TOPIC_USER_REGISTERED,
                    user.getId().toString(),
                    Map.of("userId", user.getId().toString(), "email", user.getEmail()));
            log.info("Published user.registered event for userId={}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user.registered event for userId={}: {}", user.getId(), e.getMessage());
        }

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair login(AuthRequest.Login request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(UserServiceErrorCode.INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new ServiceException(UserServiceErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ServiceException(UserServiceErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair refresh(AuthRequest.RefreshToken request) {
        Claims claims;
        try {
            claims = tokenService.validateAndExtract(request.token());
        } catch (ExpiredJwtException e) {
            throw new ServiceException(UserServiceErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new ServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        if (!tokenService.isRefreshToken(claims)) {
            throw new ServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        Token stored = tokenRepository
                .findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new ServiceException(UserServiceErrorCode.INVALID_TOKEN));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        stored.setRevoked(true);
        tokenRepository.save(stored);

        return issueTokenPair(stored.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null) {
            return;
        }
        tokenRepository
                .findByTokenHash(hash(rawToken))
                .ifPresentOrElse(
                        token -> {
                            token.setRevoked(true);
                            tokenRepository.save(token);
                            log.info(
                                    "Revoked token for user {}", token.getUser().getId());
                        },
                        () -> log.warn("Attempted to revoke unknown token"));
    }

    private AuthResponse.TokenPair issueTokenPair(User user) {
        String accessToken = tokenService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        tokenRepository.save(Token.builder()
                .user(user)
                .tokenHash(hash(refreshToken))
                .expiresAt(Instant.now().plusMillis(tokenService.getRefreshTokenExpiry()))
                .build());

        return new AuthResponse.TokenPair(accessToken, refreshToken, tokenService.getAccessTokenExpiry());
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
