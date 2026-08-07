package com.ecommerce.serivce.user.application.dto;

import com.ecommerce.serivce.user.domain.model.User;
import java.util.UUID;

public class UserResponse {
  public record UserProfile(UUID id, String email, String fullName, User.Role role) {
    public static UserProfile from(User user) {
      return new UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
  }
}
