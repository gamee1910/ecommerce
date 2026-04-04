package com.ecommerce.serivce.user.common.dto.response;

public class AuthResponse {
  public record TokenPair(String accessToken, String refreshToken, long accesssTokenExpiresIn) {}

  public record AccessToken(String accessToken, long accessTokenExpiresIn) {}
}
