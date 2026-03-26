package com.ecommerce.serivce.user.features.auth;

import com.ecommerce.serivce.user.common.dto.request.AuthRequest;
import com.ecommerce.serivce.user.common.dto.response.AuthResponse;
import com.ecommerce.serivce.user.common.exception.UserServiceErrorCode;
import com.ecommerce.serivce.user.common.exception.UserServiceException;
import com.ecommerce.serivce.user.features.token.TokeRepository;
import com.ecommerce.serivce.user.features.token.Token;
import com.ecommerce.serivce.user.features.token.TokenService;
import com.ecommerce.serivce.user.features.user.User;
import com.ecommerce.serivce.user.features.user.UserRepository;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Auth Service")
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokeRepository tokeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse.TokenPair register(AuthRequest.Register request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserServiceException(UserServiceErrorCode.EMAIL_ALREADY_EXIST);
        }

        User user = userRepository.saveOrUpdate(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .build());

        log.info("Registered new user: {}", user.getId());
        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair login(AuthRequest.Login request) {
        User user = userRepository
                .findByEmail(request.email())
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
        if (!tokenService.isValid(request.token())) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        Claims claims = tokenService.validateAndExtract(request.token());

        if (!"refresh".equals(claims.get("type"))) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        Token stored = tokeRepository
                .findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.INVALID_TOKEN));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_TOKEN);
        }

        // Token rotation: revoked old token then issue new token
        stored.setRevoked(true);
        tokeRepository.save(stored);

        User user = userRepository
                .findById(stored.getUserId())
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));

        return issueTokenPair(user);
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null) return;
        tokeRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            tokeRepository.save(token);
        });
    }

    private AuthResponse.TokenPair issueTokenPair(User user) {
        String accessToken = tokenService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String refreshToken = tokenService.generateRefreshToken(user.getId());

        tokeRepository.save(Token.builder()
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(tokenService.getAccessTokenExpiry()))
                .tokenHash(hash(refreshToken))
                .build());

        return new AuthResponse.TokenPair(accessToken, refreshToken, tokenService.getAccessTokenExpiry());
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed: ", e);
        }
    }
}
