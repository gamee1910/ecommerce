package com.ecommerce.serivce.user.common.dto.request;

public class UserRequest {
  public record Update(String fullName, boolean active) {}
}
