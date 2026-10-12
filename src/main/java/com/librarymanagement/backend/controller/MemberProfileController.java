package com.librarymanagement.backend.controller;

import com.librarymanagement.backend.dto.UserProfileRequest;
import com.librarymanagement.backend.dto.UserResponse;
import com.librarymanagement.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/profile")
public class MemberProfileController {

    private final UserService userService;

    public MemberProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getProfile(email));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication, 
            @Valid @RequestBody UserProfileRequest request) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.updateProfile(email, request));
    }
}
