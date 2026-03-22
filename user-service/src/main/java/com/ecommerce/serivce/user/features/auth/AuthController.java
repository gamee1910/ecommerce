package com.ecommerce.serivce.user.features.auth;

import com.ecommerce.serivce.user.common.dto.request.AuthRequest;
import com.ecommerce.serivce.user.common.dto.response.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse.AccessToken> login(
            @Valid @RequestBody AuthRequest.Login request, HttpServletResponse response) {
        AuthResponse.TokenPair pair = authService.login(request);
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(new AuthResponse.AccessToken(pair.accessToken(), pair.accesssTokenExpiresIn()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse.AccessToken> register(
            @Valid @RequestBody AuthRequest.Register request, HttpServletResponse response) {
        AuthResponse.TokenPair pair = authService.register(request);
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse.AccessToken(pair.accessToken(), pair.accesssTokenExpiresIn()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse.AccessToken> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        AuthResponse.TokenPair pair = authService.refresh(new AuthRequest.RefreshToken(refreshToken));

        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(new AuthResponse.AccessToken(pair.accessToken(), pair.accesssTokenExpiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            authService.revokeRefreshToken(refreshToken);
        }
        clearRefreshTokenCookie(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        response.addHeader(
                "Set-Cookie",
                String.format(
                        "%s=%s; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=%d",
                        REFRESH_TOKEN_COOKIE, token, refreshTokenExpiry / 1000));
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {

        response.addHeader(
                "Set-Cookie",
                String.format(
                        "%s=; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=0",
                        REFRESH_TOKEN_COOKIE));
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
