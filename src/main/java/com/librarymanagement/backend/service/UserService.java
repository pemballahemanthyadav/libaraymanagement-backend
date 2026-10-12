package com.librarymanagement.backend.service;

import com.librarymanagement.backend.dto.UserProfileRequest;
import com.librarymanagement.backend.dto.UserResponse;
import com.librarymanagement.backend.entity.User;
import com.librarymanagement.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponse(user);
    }

    public UserResponse updateProfile(String email, UserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        
        return new UserResponse(userRepository.save(user));
    }
}
