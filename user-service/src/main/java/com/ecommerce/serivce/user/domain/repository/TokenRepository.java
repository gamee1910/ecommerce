package com.ecommerce.serivce.user.domain.repository;

import com.ecommerce.serivce.user.domain.model.Token;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository {
    Token save(Token token);
    Optional<Token> findById(UUID id);
    Optional<Token> findByTokenHash(String tokenHash);
}
