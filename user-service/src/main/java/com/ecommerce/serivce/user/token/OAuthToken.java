package com.ecommerce.serivce.user.token;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthToken {
    private UUID id;

    private UUID userId;

    private String tokenHash;

    private String deviceInfo;

    private Instant expiresAt;

    @Builder.Default
    private boolean revoked = false;

    private Instant createdAt;
}
