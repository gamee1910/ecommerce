package com.ecommerce.service.products.service;

import com.ecommerce.service.products.config.CacheConfig;
import com.ecommerce.service.products.controller.dto.ProductPageResponse;
import com.ecommerce.service.products.controller.dto.ProductRequest;
import com.ecommerce.service.products.controller.dto.ProductResponse;
import com.ecommerce.service.products.exception.ProductServiceErrorCode;
import com.ecommerce.service.products.model.Category;
import com.ecommerce.service.products.model.Product;
import com.ecommerce.service.products.repository.CategoryRepository;
import com.ecommerce.service.products.repository.ProductRepository;
import com.gamee1910.error.exception.ServiceException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ProductService")
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_PRODUCT_KEY = "product:";
    private static final String REDIS_PAGE_PATTERN = "product-page::*";
    private static final long REDIS_TTL_SECONDS = 300;

    @Cacheable(value = CacheConfig.CACHE_PRODUCT, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {

        String redisKey = REDIS_PRODUCT_KEY + id;
        ProductResponse cached = (ProductResponse) redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            log.debug("Cache L2 hit — product:{}", id);
            return cached;
        }

        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ProductServiceErrorCode.PRODUCT_NOT_FOUND));

        ProductResponse response = toResponse(product);

        redisTemplate.opsForValue().set(redisKey, response, Duration.ofSeconds(REDIS_TTL_SECONDS));

        return response;
    }

    @Cacheable(
            value = CacheConfig.CACHE_PRODUCT_PAGE,
            key = "T(String).valueOf(#isActive) + ':' + #categoryId + ':'"
                    + "+ #minPrice + ':' + #maxPrice + ':' + #keyword + ':'"
                    + "+ #page + ':' + #size")
    @Transactional(readOnly = true)
    public ProductPageResponse getPage(
            Boolean isActive,
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String keyword,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> result =
                productRepository.findWithFilters(isActive, categoryId, minPrice, maxPrice, keyword, pageable);

        List<ProductResponse> content =
                result.getContent().stream().map(this::toResponse).toList();

        return new ProductPageResponse(
                content, page, size, result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        String slug = generateSlug(req.name());
        if (productRepository.existsBySlug(slug)) {
            throw new ServiceException(ProductServiceErrorCode.PRODUCT_SLUG_DUPLICATE);
        }

        Product product = new Product();
        applyRequest(req, product);
        product.setSlug(slug);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT, key = "#id"),
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT_PAGE, allEntries = true)
            })
    public ProductResponse update(UUID id, ProductRequest req) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ProductServiceErrorCode.PRODUCT_NOT_FOUND));

        applyRequest(req, product);
        Product saved = productRepository.save(product);

        evictProductL2(id);

        return toResponse(saved);
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT, key = "#id"),
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT_PAGE, allEntries = true)
            })
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ServiceException(ProductServiceErrorCode.PRODUCT_NOT_FOUND);
        }
        productRepository.deleteById(id);
        evictProductL2(id);
    }

    private void evictProductL2(UUID id) {
        redisTemplate.delete(REDIS_PRODUCT_KEY + id);
        Set<String> pageKeys = redisTemplate.keys(REDIS_PAGE_PATTERN);
        if (!pageKeys.isEmpty()) {
            redisTemplate.delete(pageKeys);
        }
    }

    private void applyRequest(ProductRequest req, Product product) {
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        if (req.isActive() != null) {
            product.setIsActive(req.isActive());
        }
        if (req.categoryId() != null) {
            Category cat = categoryRepository
                    .findById(req.categoryId())
                    .orElseThrow(() -> new ServiceException(ProductServiceErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(cat);
        }
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getIsActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT, key = "#id"),
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT_PAGE, allEntries = true)
            })
    public void deductStock(UUID id, int quantity) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ProductServiceErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStockQuantity() < quantity) {
            throw new ServiceException(ProductServiceErrorCode.INSUFFICIENT_STOCK);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        evictProductL2(id);

        log.info("Deducted {} units from product {} — remaining stock: {}", quantity, id, product.getStockQuantity());
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT, key = "#id"),
                @CacheEvict(value = CacheConfig.CACHE_PRODUCT_PAGE, allEntries = true)
            })
    public void restoreStock(UUID id, int quantity) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ProductServiceErrorCode.PRODUCT_NOT_FOUND));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        evictProductL2(id);

        log.info("Restored {} units to product {} — new stock: {}", quantity, id, product.getStockQuantity());
    }
}
