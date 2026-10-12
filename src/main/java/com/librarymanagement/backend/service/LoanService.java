package com.librarymanagement.backend.service;

import com.librarymanagement.backend.dto.LoanRequest;
import com.librarymanagement.backend.dto.LoanResponse;
import com.librarymanagement.backend.entity.*;
import com.librarymanagement.backend.exception.BadRequestException;
import com.librarymanagement.backend.exception.BookNotAvailableException;
import com.librarymanagement.backend.exception.BorrowLimitExceededException;
import com.librarymanagement.backend.exception.ResourceNotFoundException;
import com.librarymanagement.backend.repository.BookInventoryRepository;
import com.librarymanagement.backend.repository.LoanRepository;
import com.librarymanagement.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    /**
     * Maximum number of books a member can borrow at once.
     */
    private static final int MAX_BORROW_LIMIT = 3;

    /**
     * How many days a member has to return a book before a fine applies.
     */
    private static final int LOAN_PERIOD_DAYS = 14;

    /**
     * Fine per day (in rupees/currency units) applied after the due date.
     */
    private static final BigDecimal FINE_PER_DAY = BigDecimal.valueOf(2.00);

    private final LoanRepository loanRepository;
    private final BookInventoryRepository bookInventoryRepository;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository,
                       BookInventoryRepository bookInventoryRepository,
                       UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.bookInventoryRepository = bookInventoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Called by a MEMBER to borrow a book.
     * Uses PESSIMISTIC_WRITE lock on the inventory row to prevent concurrent double-borrow.
     *
     * @param memberEmail authenticated member's email
     * @param inventoryId the specific physical copy to borrow
     * @return the created LoanResponse
     */
    @Transactional
    public LoanResponse borrowBook(String memberEmail, Long inventoryId) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with email: " + memberEmail));

        // Enforce borrow limit
        long activeLoanCount = loanRepository.countActiveLoansForMember(member.getId());
        if (activeLoanCount >= MAX_BORROW_LIMIT) {
            throw new BorrowLimitExceededException("Borrow limit reached. You can borrow at most " + MAX_BORROW_LIMIT + " books at a time.");
        }

        // PESSIMISTIC_WRITE lock: prevents two concurrent transactions from borrowing the same copy
        BookInventory inventory = bookInventoryRepository.findByIdWithLock(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy", inventoryId));

        if (inventory.getStatus() != BookStatus.AVAILABLE) {
            throw new BookNotAvailableException("This book copy is not available for borrowing. Current status: " + inventory.getStatus());
        }

        // Check if member already has this specific copy borrowed
        List<Loan> activeLoansForCopy = loanRepository.findActiveLoansForInventory(inventoryId);
        if (!activeLoansForCopy.isEmpty()) {
            throw new BookNotAvailableException("This copy is already borrowed.");
        }

        // Mark the copy as checked out
        inventory.setStatus(BookStatus.BORROWED);
        bookInventoryRepository.save(inventory);

        // Create the loan
        Loan loan = new Loan();
        loan.setMember(member);
        loan.setInventory(inventory);
        loan.setBorrowDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(LOAN_PERIOD_DAYS));
        loan.setStatus(LoanStatus.BORROWED);
        loan.setFineAmount(BigDecimal.ZERO);

        return new LoanResponse(loanRepository.save(loan));
    }

    /**
     * Called by a MEMBER to borrow an available copy of a book by its book ID.
     * Automatically selects and locks an available physical copy.
     *
     * @param memberEmail authenticated member's email
     * @param bookId the ID of the book title to borrow
     * @return the created LoanResponse
     */
    @Transactional
    public LoanResponse borrowBookByBookId(String memberEmail, Long bookId) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with email: " + memberEmail));

        // Enforce borrow limit
        long activeLoanCount = loanRepository.countActiveLoansForMember(member.getId());
        if (activeLoanCount >= MAX_BORROW_LIMIT) {
            throw new BorrowLimitExceededException("Borrow limit reached. You can borrow at most " + MAX_BORROW_LIMIT + " books at a time.");
        }

        List<BookInventory> availableCopies = bookInventoryRepository.findByBookIdAndStatus(bookId, BookStatus.AVAILABLE);
        if (availableCopies.isEmpty()) {
            throw new BookNotAvailableException("No copies of this book are currently available for borrowing.");
        }

        // Try locking and acquiring the first available copy that isn't already borrowed
        for (BookInventory copy : availableCopies) {
            BookInventory lockedCopy = bookInventoryRepository.findByIdWithLock(copy.getId()).orElse(null);
            if (lockedCopy != null && lockedCopy.getStatus() == BookStatus.AVAILABLE) {
                List<Loan> activeLoans = loanRepository.findActiveLoansForInventory(lockedCopy.getId());
                if (activeLoans.isEmpty()) {
                    lockedCopy.setStatus(BookStatus.BORROWED);
                    bookInventoryRepository.save(lockedCopy);

                    Loan loan = new Loan();
                    loan.setMember(member);
                    loan.setInventory(lockedCopy);
                    loan.setBorrowDate(LocalDate.now());
                    loan.setDueDate(LocalDate.now().plusDays(LOAN_PERIOD_DAYS));
                    loan.setStatus(LoanStatus.BORROWED);
                    loan.setFineAmount(BigDecimal.ZERO);

                    return new LoanResponse(loanRepository.save(loan));
                }
            }
        }

        throw new BookNotAvailableException("All copies of this book are currently checked out.");
    }

    /**
     * Called by a MEMBER to return their own book.
     * Calculates and stores the fine if the book is overdue.
     *
     * @param memberEmail authenticated member's email
     * @param loanId the loan to close
     * @return updated LoanResponse
     */
    @Transactional
    public LoanResponse returnBook(String memberEmail, Long loanId) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with email: " + memberEmail));

        Loan loan = loanRepository.findByIdAndMemberId(loanId, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found or does not belong to this member"));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BadRequestException("This book has already been returned.");
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);

        // Calculate fine if overdue
        if (today.isAfter(loan.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            loan.setFineAmount(FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays)));
            loan.setStatus(LoanStatus.RETURNED);
        } else {
            loan.setFineAmount(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.RETURNED);
        }

        // Free up the physical copy
        BookInventory inventory = loan.getInventory();
        inventory.setStatus(BookStatus.AVAILABLE);
        bookInventoryRepository.save(inventory);

        return new LoanResponse(loanRepository.save(loan));
    }

    /**
     * Admin: return a book on behalf of a member.
     */
    @Transactional
    public LoanResponse adminReturnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BadRequestException("This book has already been returned.");
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);

        if (today.isAfter(loan.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            loan.setFineAmount(FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays)));
        } else {
            loan.setFineAmount(BigDecimal.ZERO);
        }

        loan.setStatus(LoanStatus.RETURNED);

        BookInventory inventory = loan.getInventory();
        inventory.setStatus(BookStatus.AVAILABLE);
        bookInventoryRepository.save(inventory);

        return new LoanResponse(loanRepository.save(loan));
    }

    /**
     * Marks all overdue loans (due date passed, still BORROWED) as OVERDUE.
     * Called by admin or a scheduled job.
     */
    @Transactional
    public int markOverdueLoans() {
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDate.now());
        for (Loan loan : overdueLoans) {
            loan.setStatus(LoanStatus.OVERDUE);
        }
        loanRepository.saveAll(overdueLoans);
        return overdueLoans.size();
    }

    // ---- Query methods ----

    @Transactional(readOnly = true)
    public Page<LoanResponse> getMemberLoans(String memberEmail, Pageable pageable) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with email: " + memberEmail));
        return loanRepository.findByMemberId(member.getId(), pageable).map(LoanResponse::new);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getMemberActiveLoans(String memberEmail) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with email: " + memberEmail));
        return loanRepository.findActiveLoansForMember(member.getId())
                .stream().map(LoanResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable).map(LoanResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoansForMember(Long memberId, Pageable pageable) {
        return loanRepository.findByMemberId(memberId, pageable).map(LoanResponse::new);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getAllOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now())
                .stream().map(LoanResponse::new).collect(Collectors.toList());
    }
}
