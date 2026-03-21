package com.ecommerce.serivce.user.user;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;

    private String email;

    private String password;

    private String fullName;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private boolean isActive = true;

    private Instant createdAt;

    private Instant updatedAt;

    public enum Role {
        USER, ADMIN
    }
}


