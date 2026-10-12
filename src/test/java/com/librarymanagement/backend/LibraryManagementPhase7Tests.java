package com.librarymanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.backend.dto.BookRequest;
import com.librarymanagement.backend.entity.*;
import com.librarymanagement.backend.repository.*;
import com.librarymanagement.backend.security.CustomUserDetails;
import com.librarymanagement.backend.security.JwtService;
import com.librarymanagement.backend.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryManagementPhase7Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookInventoryRepository bookInventoryRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private JwtService jwtService;
    @Autowired private LoanService loanService;

    private User admin;
    private User member;
    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookInventoryRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        admin = new User();
        admin.setName("Admin 7");
        admin.setEmail("admin7@test.com");
        admin.setPassword("pass");
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(admin));

        member = new User();
        member.setName("Member 7");
        member.setEmail("member7@test.com");
        member.setPassword("pass");
        member.setRole(Role.MEMBER);
        member = userRepository.save(member);
        memberToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(member));
    }

    @Test
    void testResourceNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/books/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Book not found with id: 99999")))
                .andExpect(jsonPath("$.path").value("/api/books/99999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testDuplicateResource_Returns409() throws Exception {
        Book book = new Book();
        book.setTitle("Existing Book");
        book.setAuthor("Author");
        book.setIsbn("ISBN-9999");
        book.setStatus(BookStatus.AVAILABLE);
        bookRepository.save(book);

        BookRequest request = new BookRequest();
        request.setTitle("New Book");
        request.setAuthor("Author");
        request.setIsbn("ISBN-9999");
        request.setStatus(BookStatus.AVAILABLE);

        mockMvc.perform(post("/api/admin/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void testValidationFailure_Returns400WithFieldErrors() throws Exception {
        // BookRequest with blank title and blank ISBN
        BookRequest invalidRequest = new BookRequest();
        invalidRequest.setTitle("");
        invalidRequest.setAuthor("");
        invalidRequest.setIsbn("");

        mockMvc.perform(post("/api/admin/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.author").exists())
                .andExpect(jsonPath("$.validationErrors.isbn").exists());
    }

    @Test
    void testBorrowUnavailableBook_Returns400() throws Exception {
        Book book = new Book();
        book.setTitle("Unavailable Book");
        book.setAuthor("Author");
        book.setIsbn("ISBN-UNAVAIL");
        book.setStatus(BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        BookInventory copy = new BookInventory();
        copy.setBook(book);
        copy.setAccessionNumber("ACC-UNAVAIL");
        copy.setStatus(BookStatus.UNAVAILABLE);
        copy = bookInventoryRepository.save(copy);

        mockMvc.perform(post("/api/member/loans/borrow")
                .header("Authorization", memberToken)
                .param("inventoryId", String.valueOf(copy.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("not available for borrowing")));
    }

    @Test
    void testBorrowLimitExceeded_Returns400() throws Exception {
        // Borrow 3 books (the max limit)
        for (int i = 1; i <= 3; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author");
            book.setIsbn("ISBN-L-" + i);
            book.setStatus(BookStatus.AVAILABLE);
            book = bookRepository.save(book);

            BookInventory copy = new BookInventory();
            copy.setBook(book);
            copy.setAccessionNumber("ACC-L-" + i);
            copy.setStatus(BookStatus.AVAILABLE);
            copy = bookInventoryRepository.save(copy);

            loanService.borrowBook(member.getEmail(), copy.getId());
        }

        // Try borrowing a 4th book
        Book fourthBook = new Book();
        fourthBook.setTitle("Book 4");
        fourthBook.setAuthor("Author");
        fourthBook.setIsbn("ISBN-L-4");
        fourthBook.setStatus(BookStatus.AVAILABLE);
        fourthBook = bookRepository.save(fourthBook);

        BookInventory fourthCopy = new BookInventory();
        fourthCopy.setBook(fourthBook);
        fourthCopy.setAccessionNumber("ACC-L-4");
        fourthCopy.setStatus(BookStatus.AVAILABLE);
        fourthCopy = bookInventoryRepository.save(fourthCopy);

        mockMvc.perform(post("/api/member/loans/borrow")
                .header("Authorization", memberToken)
                .param("inventoryId", String.valueOf(fourthCopy.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Borrow limit reached")));
    }

    @Test
    void testReturnAlreadyReturnedBook_Returns400() throws Exception {
        Book book = new Book();
        book.setTitle("Return Book");
        book.setAuthor("Author");
        book.setIsbn("ISBN-RET");
        book.setStatus(BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        BookInventory copy = new BookInventory();
        copy.setBook(book);
        copy.setAccessionNumber("ACC-RET");
        copy.setStatus(BookStatus.AVAILABLE);
        copy = bookInventoryRepository.save(copy);

        var loan = loanService.borrowBook(member.getEmail(), copy.getId());
        // Return once
        loanService.returnBook(member.getEmail(), loan.getId());

        // Return second time via endpoint
        mockMvc.perform(post("/api/member/loans/{loanId}/return", loan.getId())
                .header("Authorization", memberToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("already been returned")));
    }
}
