package com.ecommerce.serivce.users.service;

import com.ecommerce.serivce.users.controller.dto.UserRequest;
import com.ecommerce.serivce.users.controller.dto.UserResponse;
import com.ecommerce.serivce.users.exception.UserServiceErrorCode;
import com.ecommerce.serivce.users.model.User;
import com.ecommerce.serivce.users.repository.UserRepository;
import com.gamee1910.error.exception.ServiceException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "UserService")
public class UserService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#userId")
    public UserResponse.UserProfile findByUserId(UUID userId) {
        return userRepository
                .findById(userId)
                .map(UserResponse.UserProfile::from)
                .orElseThrow(() -> new ServiceException(UserServiceErrorCode.USER_NOT_FOUND));
    }

    public UserResponse.UserProfile findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .map(UserResponse.UserProfile::from)
                .orElseThrow(() -> new ServiceException(UserServiceErrorCode.USER_NOT_FOUND));
    }

    public UserResponse.UserProfile getCurrentUser() {
        Authentication auth = requireAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        return userRepository
                .findById(userId)
                .map(UserResponse.UserProfile::from)
                .orElseThrow(() -> new ServiceException(UserServiceErrorCode.INVALID_CREDENTIALS));
    }

    @CachePut(value = "users", key = "#userId")
    public UserResponse.UserProfile update(UUID userId, UserRequest.Update request) {
        Authentication auth = requireAuthentication();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));

        if (!isAdmin && !auth.getName().equals(userId.toString())) {
            throw new ServiceException(UserServiceErrorCode.FORBIDDEN);
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ServiceException(UserServiceErrorCode.USER_NOT_FOUND));

        user.setActive(request.active());
        user.setFullName(request.fullName());

        return UserResponse.UserProfile.from(userRepository.save(user));
    }

    private Authentication requireAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ServiceException(UserServiceErrorCode.INVALID_CREDENTIALS);
        }
        return auth;
    }
}
