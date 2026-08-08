package com.ecommerce.serivce.users.controller.dto;

public class AuthResponse {
    public record TokenPair(String accessToken, String refreshToken, long accesssTokenExpiresIn) {}

    public record AccessToken(String accessToken, long accessTokenExpiresIn) {}
}
