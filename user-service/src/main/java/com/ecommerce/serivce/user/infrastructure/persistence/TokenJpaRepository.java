package com.ecommerce.serivce.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenJpaRepository extends JpaRepository<TokenEntity, UUID> {

  Optional<TokenEntity> findByTokenHash(String tokenHash);

}
