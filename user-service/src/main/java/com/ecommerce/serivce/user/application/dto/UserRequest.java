package com.ecommerce.serivce.user.application.dto;

public class UserRequest {
  public record Update(String fullName, boolean active) {}
}
