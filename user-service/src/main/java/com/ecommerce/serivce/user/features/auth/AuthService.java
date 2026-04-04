package com.ecommerce.serivce.user.features.auth;

import com.ecommerce.serivce.user.common.dto.request.AuthRequest;
import com.ecommerce.serivce.user.common.dto.response.AuthResponse;
import com.ecommerce.serivce.user.common.exception.UserServiceErrorCode;
import com.ecommerce.serivce.user.common.exception.UserServiceException;
import com.ecommerce.serivce.user.features.token.Token;
import com.ecommerce.serivce.user.features.token.TokenRepository;
import com.ecommerce.serivce.user.features.token.TokenService;
import com.ecommerce.serivce.user.features.user.User;
import com.ecommerce.serivce.user.features.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Auth Service")
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse.TokenPair register(AuthRequest.Register request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserServiceException(UserServiceErrorCode.EMAIL_ALREADY_EXIST);
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .build());

        log.info("Registered new user: {}", user.getId());
        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair login(AuthRequest.Login request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new UserServiceException(UserServiceErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair refresh(AuthRequest.RefreshToken request) {
        Claims claims;
        try {
            claims = tokenService.validateAndExtract(request.token());
        } catch (ExpiredJwtException e) {
            throw new UserServiceException(UserServiceErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        if (!tokenService.isRefreshToken(claims)) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        Token stored = tokenRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.INVALID_TOKEN));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        // Token rotation: revoke old, issue new
        stored.setRevoked(true);
        tokenRepository.save(stored);

        return issueTokenPair(stored.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null) return;
        tokenRepository.findByTokenHash(hash(rawToken)).ifPresentOrElse(
                token -> {
                    token.setRevoked(true);
                    tokenRepository.save(token);
                    log.info("Revoked token for user {}", token.getUser().getId());
                },
                () -> log.warn("Attempted to revoke unknown token")
        );
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
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
