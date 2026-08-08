package com.ecommerce.service.products.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", message = "Price must be >= 0") BigDecimal price,
        @NotNull @Min(value = 0, message = "Stock must be >= 0") Integer stockQuantity,
        UUID categoryId,
        Boolean isActive) {}
