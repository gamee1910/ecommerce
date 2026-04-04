package com.ecommerce.service.product.common.dto.request;

import java.math.BigDecimal;

public record CreateProductRequest(
    String name, Long categoryId, String description, BigDecimal price, Integer stock) {}
