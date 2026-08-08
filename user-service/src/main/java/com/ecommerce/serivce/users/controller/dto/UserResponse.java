package com.ecommerce.serivce.users.controller.dto;

import com.ecommerce.serivce.users.model.User;
import java.util.UUID;

public class UserResponse {
    public record UserProfile(UUID id, String email, String fullName, User.Role role) {
        public static UserProfile from(User user) {
            return new UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }
}
