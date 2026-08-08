package com.ecommerce.service.products.repository;

import com.ecommerce.service.products.model.Product;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query(
            """
      SELECT p FROM Product p
      LEFT JOIN FETCH p.category c
      WHERE (:isActive IS NULL    OR p.isActive = :isActive)
        AND (:categoryId IS NULL  OR c.id = :categoryId)
        AND (:minPrice IS NULL    OR p.price >= :minPrice)
        AND (:maxPrice IS NULL    OR p.price <= :maxPrice)
        AND (:keyword IS NULL
             OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
      """)
    Page<Product> findWithFilters(
            @Param("isActive") Boolean isActive,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            Pageable pageable);

    boolean existsBySlug(String slug);
}
