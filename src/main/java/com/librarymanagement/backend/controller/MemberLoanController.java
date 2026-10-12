package com.librarymanagement.backend.controller;

import com.librarymanagement.backend.dto.LoanResponse;
import com.librarymanagement.backend.service.LoanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
public class MemberLoanController {

    private final LoanService loanService;

    public MemberLoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * POST /api/member/loans/borrow?inventoryId={id} OR ?bookId={id}
     * Member borrows a specific copy or an available copy of a book.
     */
    @PostMapping("/loans/borrow")
    public ResponseEntity<LoanResponse> borrowBook(
            Authentication authentication,
            @RequestParam(required = false) Long inventoryId,
            @RequestParam(required = false) Long bookId) {
        String email = authentication.getName();
        if (inventoryId != null) {
            return ResponseEntity.ok(loanService.borrowBook(email, inventoryId));
        } else if (bookId != null) {
            return ResponseEntity.ok(loanService.borrowBookByBookId(email, bookId));
        } else {
            throw new com.librarymanagement.backend.exception.BadRequestException(
                    "Either inventoryId or bookId must be provided to borrow a book.");
        }
    }

    /**
     * POST /api/member/books/{bookId}/borrow
     * Member borrows an available copy of a book directly by book ID.
     */
    @PostMapping("/books/{bookId}/borrow")
    public ResponseEntity<LoanResponse> borrowBookByBookId(
            Authentication authentication,
            @PathVariable Long bookId) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.borrowBookByBookId(email, bookId));
    }

    /**
     * POST /api/member/copies/{copyId}/borrow
     * Member borrows a specific physical copy of a book.
     */
    @PostMapping("/copies/{copyId}/borrow")
    public ResponseEntity<LoanResponse> borrowCopy(
            Authentication authentication,
            @PathVariable Long copyId) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.borrowBook(email, copyId));
    }

    /**
     * POST /api/member/loans/{loanId}/return
     * Member returns their own borrowed book.
     */
    @PostMapping("/loans/{loanId}/return")
    public ResponseEntity<LoanResponse> returnBook(
            Authentication authentication,
            @PathVariable Long loanId) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.returnBook(email, loanId));
    }

    /**
     * GET /api/member/loans
     * Paginated list of all the member's loans.
     */
    @GetMapping("/loans")
    public ResponseEntity<Page<LoanResponse>> getMyLoans(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.getMemberLoans(email, PageRequest.of(page, size)));
    }

    /**
     * GET /api/member/loans/active
     * Returns all currently active (BORROWED or OVERDUE) loans for the member.
     */
    @GetMapping("/loans/active")
    public ResponseEntity<List<LoanResponse>> getMyActiveLoans(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(loanService.getMemberActiveLoans(email));
    }
}
