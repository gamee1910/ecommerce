package com.ecommerce.serivce.user.features.user;

import com.ecommerce.serivce.user.common.dto.request.UserRequest;
import com.ecommerce.serivce.user.common.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse.UserProfile> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.findByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<UserResponse.UserProfile> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse.UserProfile> update(
            @PathVariable String userId, @Valid @RequestBody UserRequest.Update request) {
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @GetMapping("/info")
    public ResponseEntity<UserResponse.UserProfile> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }
}
