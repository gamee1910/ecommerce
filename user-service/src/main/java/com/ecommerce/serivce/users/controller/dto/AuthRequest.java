package com.ecommerce.serivce.users.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public record Register(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 length characters") String password,
            @NotBlank String fullName) {}

    public record Login(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshToken(@NotBlank String token) {}
}
