package com.ecommerce.serivce.user.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private UUID id;
    private User user;
    private String tokenHash;
    private String deviceInfo;
    private Instant expiresAt;
    @Builder.Default
    private boolean revoked = false;
    private Instant createdAt;
}
