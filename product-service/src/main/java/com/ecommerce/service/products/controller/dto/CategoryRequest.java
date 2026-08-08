package com.ecommerce.service.products.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CategoryRequest(@NotBlank String name, UUID parentId) {}
