package com.librarymanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.backend.dto.AuthRequest;
import com.librarymanagement.backend.dto.RegisterRequest;
import com.librarymanagement.backend.entity.Role;
import com.librarymanagement.backend.entity.User;
import com.librarymanagement.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryManagementPhase4Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerMember_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "John Doe", "john@example.com", "password123", "1234567890"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.id").exists());

        assertTrue(userRepository.existsByEmail("john@example.com"));
    }

    @Test
    void registerMember_DuplicateEmail_ShouldFail() throws Exception {
        // Pre-create user
        User existingUser = new User();
        existingUser.setName("Jane Doe");
        existingUser.setEmail("jane@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setRole(Role.MEMBER);
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest(
                "Another Jane", "jane@example.com", "password123", "0987654321"
        );

        // GlobalExceptionHandler catches DuplicateResourceException and returns 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email already exists")));
    }

    @Test
    void loginMember_Success() throws Exception {
        // Pre-create user
        User existingUser = new User();
        existingUser.setName("Alice");
        existingUser.setEmail("alice@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setRole(Role.MEMBER);
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest("alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void loginMember_WrongPassword_ShouldFail() throws Exception {
        // Pre-create user
        User existingUser = new User();
        existingUser.setName("Bob");
        existingUser.setEmail("bob@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setRole(Role.MEMBER);
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest("bob@example.com", "wrongpassword");

        // GlobalExceptionHandler catches BadCredentialsException and returns 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
