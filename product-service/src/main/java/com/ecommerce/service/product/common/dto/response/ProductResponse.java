package com.ecommerce.service.product.common.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String slug,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    String categoryName) {}
