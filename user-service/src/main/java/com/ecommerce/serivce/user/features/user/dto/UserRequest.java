package com.ecommerce.serivce.user.features.user.dto;

public class UserRequest {
  public record Update(String fullName, boolean active) {}
}
