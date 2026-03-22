package com.ecommerce.serivce.user.features.user;

import com.ecommerce.serivce.user.common.dto.request.UserRequest;
import com.ecommerce.serivce.user.common.dto.response.UserResponse;
import com.ecommerce.serivce.user.common.exception.UserServiceErrorCode;
import com.ecommerce.serivce.user.common.exception.UserServiceException;
import java.util.Objects;
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

    private final UserRepository userRepository;

    public UserResponse.UserProfile findByUserId(String userId) {
        User user = userRepository
                .findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));

        return new UserResponse.UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public UserResponse.UserProfile findByEmail(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));
        return new UserResponse.UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public UserResponse.UserProfile getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS);
        }

        String UUIDsubject = authentication.getName();
        User user = userRepository
                .findById(UUID.fromString(UUIDsubject))
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS));

        return new UserResponse.UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public UserResponse.UserProfile update(String userId, UserRequest.Update request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UserServiceException(UserServiceErrorCode.INVALID_CREDENTIALS);
        }

        String currentUserId = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (!isAdmin && !currentUserId.equals(userId)) {
            throw new UserServiceException(UserServiceErrorCode.FORBIDDEN);
        }
        User user = userRepository
                .findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserServiceException(UserServiceErrorCode.USER_NOT_FOUND));

        user.setActive(request.active());
        user.setFullName(request.fullName());

        User update = userRepository.saveOrUpdate(user);

        return new UserResponse.UserProfile(update.getId(), update.getEmail(), update.getFullName(), update.getRole());
    }
}
