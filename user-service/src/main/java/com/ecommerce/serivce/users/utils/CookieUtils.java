package com.ecommerce.serivce.users.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    private static final long MILLIS_PER_SECOND = 1000L;

    public void setRefreshCookie(HttpServletResponse response, String token, long expiryMs) {
        response.addHeader(
                "Set-Cookie",
                String.format(
                        "%s=%s; HttpOnly; Secure; SameSite=Strict; Path=%s; Max-Age=%d",
                        REFRESH_TOKEN_COOKIE, token, REFRESH_COOKIE_PATH, expiryMs / MILLIS_PER_SECOND));
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(
                "Set-Cookie",
                String.format(
                        "%s=; HttpOnly; Secure; SameSite=Strict; Path=%s; Max-Age=0",
                        REFRESH_TOKEN_COOKIE, REFRESH_COOKIE_PATH));
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
