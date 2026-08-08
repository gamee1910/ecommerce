package com.ecommerce.serivce.users.controller.dto;

public class UserRequest {
    public record Update(String fullName, boolean active) {}
}
