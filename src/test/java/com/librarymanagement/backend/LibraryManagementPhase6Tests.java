package com.librarymanagement.backend;

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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryManagementPhase6Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookInventoryRepository bookInventoryRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private JwtService jwtService;
    @Autowired private LoanService loanService;

    private User member;
    private User admin;
    private Book book;
    private BookInventory copy;
    private String memberToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookInventoryRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        admin = new User();
        admin.setName("Admin");
        admin.setEmail("admin6@test.com");
        admin.setPassword("pass");
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(admin));

        member = new User();
        member.setName("Member");
        member.setEmail("member6@test.com");
        member.setPassword("pass");
        member.setRole(Role.MEMBER);
        member = userRepository.save(member);
        memberToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(member));

        book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("ISBN-0001");
        book.setStatus(BookStatus.AVAILABLE);
        book = bookRepository.save(book);

        copy = new BookInventory();
        copy.setBook(book);
        copy.setAccessionNumber("ACC-001");
        copy.setStatus(BookStatus.AVAILABLE);
        copy = bookInventoryRepository.save(copy);
    }

    @Test
    void memberCanBorrowBook_Success() throws Exception {
        mockMvc.perform(post("/api/member/loans/borrow")
                .header("Authorization", memberToken)
                .param("inventoryId", String.valueOf(copy.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookTitle").value("Test Book"))
                .andExpect(jsonPath("$.status").value("BORROWED"));

        BookInventory updated = bookInventoryRepository.findById(copy.getId()).orElseThrow();
        assertEquals(BookStatus.BORROWED, updated.getStatus());
    }

    @Test
    void memberCannotBorrowAlreadyBorrowedCopy() {
        // First borrow
        loanService.borrowBook(member.getEmail(), copy.getId());

        // Second attempt on same copy — copy is now BORROWED status so it should fail
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanService.borrowBook(member.getEmail(), copy.getId()));
        assertTrue(ex.getMessage().contains("not available"));
    }

    @Test
    void memberCanReturnBook_Success() throws Exception {
        // Borrow first
        var loan = loanService.borrowBook(member.getEmail(), copy.getId());

        // Return
        mockMvc.perform(post("/api/member/loans/{loanId}/return", loan.getId())
                .header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fineAmount").value(0));

        BookInventory updated = bookInventoryRepository.findById(copy.getId()).orElseThrow();
        assertEquals(BookStatus.AVAILABLE, updated.getStatus());
    }

    @Test
    void overdueFineShouldBeCalculated() {
        // Manually create an overdue loan (due date in the past)
        Loan loan = new Loan();
        loan.setMember(member);
        loan.setInventory(copy);
        loan.setBorrowDate(LocalDate.now().minusDays(20));
        loan.setDueDate(LocalDate.now().minusDays(6));  // 6 days overdue
        loan.setStatus(LoanStatus.BORROWED);
        loan.setFineAmount(BigDecimal.ZERO);
        loan = loanRepository.save(loan);

        // Mark copy as borrowed
        copy.setStatus(BookStatus.BORROWED);
        bookInventoryRepository.save(copy);

        var returned = loanService.returnBook(member.getEmail(), loan.getId());
        assertTrue(returned.getFineAmount().compareTo(BigDecimal.valueOf(12.00)) == 0,
                "Fine should be 6 days × 2.00 = 12.00, but was: " + returned.getFineAmount());
    }

    @Test
    void adminCanViewAllLoans() throws Exception {
        loanService.borrowBook(member.getEmail(), copy.getId());

        mockMvc.perform(get("/api/admin/loans")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminCanMarkOverdueLoans() {
        // Create a loan that is past due
        Loan loan = new Loan();
        loan.setMember(member);
        loan.setInventory(copy);
        loan.setBorrowDate(LocalDate.now().minusDays(20));
        loan.setDueDate(LocalDate.now().minusDays(6));
        loan.setStatus(LoanStatus.BORROWED);
        loan.setFineAmount(BigDecimal.ZERO);
        loanRepository.save(loan);

        int updated = loanService.markOverdueLoans();
        assertEquals(1, updated);

        Loan updatedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertEquals(LoanStatus.OVERDUE, updatedLoan.getStatus());
    }
}
