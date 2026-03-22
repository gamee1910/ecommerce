package com.ecommerce.serivce.user.common.dto.response;

import com.ecommerce.serivce.user.features.user.User;
import java.util.UUID;

public class UserResponse {
    public record UserProfile(UUID id, String email, String fullName, User.Role role) {}
}
