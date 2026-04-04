package com.ecommerce.service.product.features.products;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  String FIND_ALL_ACTIVE_QUERY =
      "SELECT p FROM Product p JOIN FETCH p.category WHERE (:categoryId IS NULL OR p.category.id = :categoryId) AND p.isActive = true";

  String FULL_TEXT_SEARCH_QUERY =
      """
              SELECT * FROM products WHERE to_tsvector('english', name) @@ plainto_tsquery('english', :query)
              AND is_active = true
              ORDER BY ts_rank(to_tsvector('english', name), plainto_tsquery('english', :query)) DESC
              LIMIT :limit OFFSET :offset
              """;

  Optional<Product> findBySlugAndIsActive(String slug, Boolean isActive);

  @Query(value = FULL_TEXT_SEARCH_QUERY, nativeQuery = true)
  List<Product> fullTextSearch(
      @Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

  @Query(FIND_ALL_ACTIVE_QUERY)
  Page<Product> findAllActive(@Param("categoryId") UUID categoryId, Pageable pageable);
}
