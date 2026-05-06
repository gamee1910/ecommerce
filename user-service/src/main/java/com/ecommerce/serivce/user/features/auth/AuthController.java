package com.ecommerce.serivce.user.features.auth;

import com.ecommerce.serivce.user.features.auth.dto.AuthRequest;
import com.ecommerce.serivce.user.features.auth.dto.AuthResponse;
import com.ecommerce.serivce.user.common.exception.UserServiceErrorCode;
import com.ecommerce.serivce.user.common.exception.UserServiceException;
import com.ecommerce.serivce.user.common.utils.CookieUtils;
import com.ecommerce.serivce.user.features.auth.service.IAuthService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final CookieUtils cookieUtils;

  @Value("${app.jwt.refresh-token-expiry}")
  private long refreshTokenExpiry;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse.AccessToken> register(
      @Valid @RequestBody AuthRequest.Register request, HttpServletResponse response) {

    AuthResponse.TokenPair pair = authService.register(request);
    cookieUtils.setRefreshCookie(response, pair.refreshToken(), refreshTokenExpiry);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new AuthResponse.AccessToken(pair.accessToken(), pair.accesssTokenExpiresIn()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse.AccessToken> login(
      @Valid @RequestBody AuthRequest.Login request, HttpServletResponse response) {

    AuthResponse.TokenPair pair = authService.login(request);
    cookieUtils.setRefreshCookie(response, pair.refreshToken(), refreshTokenExpiry);
    return ResponseEntity.ok(
        new AuthResponse.AccessToken(pair.accessToken(), pair.accesssTokenExpiresIn()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse.AccessToken> refresh(
      HttpServletRequest request, HttpServletResponse response) {

    String refreshToken =
        cookieUtils
            .extractRefreshToken(request)
            .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.INVALID_TOKEN));

    AuthResponse.TokenPair pair = authService.refresh(new AuthRequest.RefreshToken(refreshToken));
    cookieUtils.setRefreshCookie(response, pair.refreshToken(), refreshTokenExpiry);
    return ResponseEntity.ok(
        new AuthResponse.AccessToken(pair.accessToken(), pair.accesssTokenExpiresIn()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

    cookieUtils.extractRefreshToken(request).ifPresent(authService::revokeRefreshToken);

    cookieUtils.clearRefreshCookie(response);
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }
}
