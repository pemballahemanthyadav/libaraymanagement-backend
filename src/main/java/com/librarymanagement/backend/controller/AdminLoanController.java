package com.librarymanagement.backend.controller;

import com.librarymanagement.backend.dto.LoanResponse;
import com.librarymanagement.backend.service.LoanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/loans")
public class AdminLoanController {

    private final LoanService loanService;

    public AdminLoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * GET /api/admin/loans
     * Paginated list of ALL loans in the system.
     */
    @GetMapping
    public ResponseEntity<Page<LoanResponse>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(loanService.getAllLoans(PageRequest.of(page, size)));
    }

    /**
     * GET /api/admin/loans/member/{memberId}
     * Paginated list of all loans for a specific member.
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Page<LoanResponse>> getLoansForMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(loanService.getLoansForMember(memberId, PageRequest.of(page, size)));
    }

    /**
     * GET /api/admin/loans/overdue
     * Returns all currently overdue loans.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<LoanResponse>> getOverdueLoans() {
        return ResponseEntity.ok(loanService.getAllOverdueLoans());
    }

    /**
     * POST /api/admin/loans/{loanId}/return
     * Admin processes a return on behalf of a member.
     */
    @PostMapping("/{loanId}/return")
    public ResponseEntity<LoanResponse> adminReturnBook(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.adminReturnBook(loanId));
    }

    /**
     * POST /api/admin/loans/mark-overdue
     * Scans all active loans past their due date and marks them OVERDUE.
     * Returns how many records were updated.
     */
    @PostMapping("/mark-overdue")
    public ResponseEntity<Map<String, Integer>> markOverdueLoans() {
        int count = loanService.markOverdueLoans();
        return ResponseEntity.ok(Map.of("updatedCount", count));
    }
}
