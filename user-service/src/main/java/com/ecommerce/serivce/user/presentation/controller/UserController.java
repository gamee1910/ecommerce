package com.ecommerce.serivce.user.presentation.controller;

import com.ecommerce.serivce.user.application.dto.UserRequest;
import com.ecommerce.serivce.user.application.dto.UserResponse;
import com.ecommerce.serivce.user.application.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserResponse.UserProfile> getCurrentUser() {
    return ResponseEntity.ok(userService.getCurrentUser());
  }

  @GetMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
  public ResponseEntity<UserResponse.UserProfile> getUserById(@PathVariable UUID userId) {
    return ResponseEntity.ok(userService.findByUserId(userId));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse.UserProfile> getUserByEmail(@RequestParam String email) {
    return ResponseEntity.ok(userService.findByEmail(email));
  }

  @PutMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
  public ResponseEntity<UserResponse.UserProfile> update(
      @PathVariable UUID userId, @Valid @RequestBody UserRequest.Update request) {
    return ResponseEntity.ok(userService.update(userId, request));
  }
}
