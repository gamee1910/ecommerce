package com.ecommerce.serivce.user.features.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.revoked = true WHERE t.user.id = :userId")
    void revokeAllTokensByUserId(@Param("userId") UUID userId);
}
