package com.ecommerce.service.products.service;

import com.ecommerce.service.products.config.CacheConfig;
import com.ecommerce.service.products.controller.dto.CategoryRequest;
import com.ecommerce.service.products.controller.dto.CategoryResponse;
import com.ecommerce.service.products.exception.ProductServiceErrorCode;
import com.ecommerce.service.products.model.Category;
import com.ecommerce.service.products.repository.CategoryRepository;
import com.gamee1910.error.exception.ServiceException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = CacheConfig.CACHE_CATEGORY, key = "'all-roots'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllRootWithChildren().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORY, allEntries = true)
    public CategoryResponse create(CategoryRequest req) {
        Category cat = new Category();
        cat.setName(req.name());
        cat.setSlug(generateSlug(req.name()));
        if (req.parentId() != null) {
            Category parent = categoryRepository
                    .findById(req.parentId())
                    .orElseThrow(() -> new ServiceException(ProductServiceErrorCode.CATEGORY_NOT_FOUND));
            cat.setParent(parent);
        }
        return toResponse(categoryRepository.save(cat));
    }

    private CategoryResponse toResponse(Category c) {
        List<CategoryResponse> children = c.getChildren() == null
                ? List.of()
                : c.getChildren().stream().map(this::toResponse).toList();
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getParent() != null ? c.getParent().getId() : null,
                children);
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
    }
}
