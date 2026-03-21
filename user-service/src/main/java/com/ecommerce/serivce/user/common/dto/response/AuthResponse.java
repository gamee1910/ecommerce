package com.ecommerce.serivce.user.common.dto.response;

import com.ecommerce.serivce.user.features.user.User;
import java.util.UUID;

public class AuthResponse {
    public record TokenPair(String accessToken, String refreshToken, long accesssTokenExpiresIn) {}

    public record AccessToken(String accessToken, long accessTokenExpiresIn) {}

    public record UserProfile(UUID id, String email, String fullName, User.Role role) {}
}
