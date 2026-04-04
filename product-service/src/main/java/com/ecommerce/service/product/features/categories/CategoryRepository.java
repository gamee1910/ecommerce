package com.ecommerce.service.product.features.categories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  Optional<Category> findBySlug(String slug);

  List<Category> findByParentIdIsNull();

  List<Category> findByParentId(UUID parentId);
}
