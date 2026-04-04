package com.ecommerce.serivce.user.features.user;

import com.ecommerce.serivce.user.common.dto.request.UserRequest;
import com.ecommerce.serivce.user.common.dto.response.UserResponse;
import com.ecommerce.serivce.user.common.exception.UserServiceErrorCode;
import com.ecommerce.serivce.user.common.exception.UserServiceException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "UserService")
public class UserService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    public UserResponse.UserProfile findByUserId(UUID userId) {
        return userRepository
                .findById(userId)
                .map(UserResponse.UserProfile::from)
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));
    }

    public UserResponse.UserProfile findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .map(UserResponse.UserProfile::from)
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));
    }

    public UserResponse.UserProfile getCurrentUser() {
        Authentication auth = requireAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        return userRepository
                .findById(userId)
                .map(UserResponse.UserProfile::from)
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS));
    }

    public UserResponse.UserProfile update(UUID userId, UserRequest.Update request) {
        Authentication auth = requireAuthentication();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));

        if (!isAdmin && !auth.getName().equals(userId.toString())) {
            throw new UserServiceException(UserServiceErrorCode.FORBIDDEN);
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));

        user.setActive(request.active());
        user.setFullName(request.fullName());

        return UserResponse.UserProfile.from(userRepository.save(user));
    }

    private Authentication requireAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS);
        }
        return auth;
    }
}
