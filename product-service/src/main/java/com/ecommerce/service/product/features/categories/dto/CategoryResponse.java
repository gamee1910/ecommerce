package com.ecommerce.service.product.features.categories.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, UUID parentId, List<CategoryResponse> children)
        implements Serializable {}
