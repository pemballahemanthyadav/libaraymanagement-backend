package com.librarymanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.backend.dto.BookRequest;
import com.librarymanagement.backend.dto.BookInventoryRequest;
import com.librarymanagement.backend.entity.BookStatus;
import com.librarymanagement.backend.entity.Role;
import com.librarymanagement.backend.entity.User;
import com.librarymanagement.backend.repository.BookInventoryRepository;
import com.librarymanagement.backend.repository.BookRepository;
import com.librarymanagement.backend.repository.UserRepository;
import com.librarymanagement.backend.security.JwtService;
import com.librarymanagement.backend.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryManagementPhase5Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookInventoryRepository bookInventoryRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        bookInventoryRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setPassword("pass");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(admin));

        User member = new User();
        member.setName("Member");
        member.setEmail("member@example.com");
        member.setPassword("pass");
        member.setRole(Role.MEMBER);
        userRepository.save(member);
        memberToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(member));
    }

    @Test
    void adminCanAddBook_Success() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("Effective Java");
        request.setAuthor("Joshua Bloch");
        request.setIsbn("978-0134685991");
        request.setStatus(BookStatus.AVAILABLE);

        mockMvc.perform(post("/api/admin/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Effective Java"));
    }

    @Test
    void memberCannotAddBook_Forbidden() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("Effective Java");
        request.setAuthor("Joshua Bloch");
        request.setIsbn("978-0134685991");

        mockMvc.perform(post("/api/admin/books")
                .header("Authorization", memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicCanBrowseBooks_Success() throws Exception {
        // Create a book first using service/repo
        BookRequest request = new BookRequest();
        request.setTitle("Clean Code");
        request.setAuthor("Robert C. Martin");
        request.setIsbn("978-0132350884");
        request.setStatus(BookStatus.AVAILABLE);
        
        mockMvc.perform(post("/api/admin/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Browse publicly (no token)
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }
}
