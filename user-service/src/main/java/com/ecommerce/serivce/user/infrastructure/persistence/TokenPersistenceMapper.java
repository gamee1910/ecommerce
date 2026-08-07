package com.ecommerce.serivce.user.infrastructure.persistence;

import com.ecommerce.serivce.user.domain.model.Token;
import org.springframework.stereotype.Component;

@Component
public class TokenPersistenceMapper {

    private final UserPersistenceMapper userPersistenceMapper;

    public TokenPersistenceMapper(UserPersistenceMapper userPersistenceMapper) {
        this.userPersistenceMapper = userPersistenceMapper;
    }

    public Token toDomain(TokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return Token.builder()
                .id(entity.getId())
                .user(userPersistenceMapper.toDomain(entity.getUser()))
                .tokenHash(entity.getTokenHash())
                .deviceInfo(entity.getDeviceInfo())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.isRevoked())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public TokenEntity toEntity(Token domain) {
        if (domain == null) {
            return null;
        }
        return TokenEntity.builder()
                .id(domain.getId())
                .user(userPersistenceMapper.toEntity(domain.getUser()))
                .tokenHash(domain.getTokenHash())
                .deviceInfo(domain.getDeviceInfo())
                .expiresAt(domain.getExpiresAt())
                .revoked(domain.isRevoked())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
