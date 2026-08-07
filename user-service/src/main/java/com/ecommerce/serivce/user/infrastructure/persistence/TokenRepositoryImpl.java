package com.ecommerce.serivce.user.infrastructure.persistence;

import com.ecommerce.serivce.user.domain.model.Token;
import com.ecommerce.serivce.user.domain.repository.TokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class TokenRepositoryImpl implements TokenRepository {

    private final TokenJpaRepository jpaRepository;
    private final TokenPersistenceMapper mapper;

    public TokenRepositoryImpl(TokenJpaRepository jpaRepository, TokenPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Token save(Token token) {
        TokenEntity entity = mapper.toEntity(token);
        TokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Token> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Token> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }
}
