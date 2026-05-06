package com.ecommerce.service.product.features.products.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        UUID categoryId,
        String categoryName,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt)
        implements Serializable {}
