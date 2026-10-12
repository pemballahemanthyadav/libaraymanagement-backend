package com.librarymanagement.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan Entity - represents a borrowing transaction in the Library Management System.
 *
 * A Loan record is created when a member borrows a specific physical copy (BookInventory).
 * The record persists forever — historical loans are NEVER deleted.
 * When a book is returned, the Loan is updated (returnDate set, status -> RETURNED),
 * but the record stays in the database for history and statistics.
 *
 * Fine calculation:
 *   fineAmount is stored on the Loan itself (no separate Fine entity).
 *   This is sufficient for a college project. If payment processing were needed,
 *   a separate Fine entity with payment status would be appropriate.
 *   Fine calculation: (returnDate or today - dueDate) * daily rate (if overdue).
 *   Only server-side logic can update fineAmount — never a client API call.
 *
 * Concurrent duplicate-borrow prevention (see BookInventory for full explanation):
 *   The Loan table does NOT have a unique constraint on (member_id, inventory_id)
 *   because that would prevent a member from ever borrowing the same copy after returning it.
 *   Instead, the LoanService uses pessimistic locking (SELECT ... FOR UPDATE) on the
 *   BookInventory row and checks for active loans before creating a new Loan.
 *   An active loan is defined as: Loan where inventory_id = X AND status IN (BORROWED, OVERDUE).
 *
 * Maps to the "loans" table in MySQL.
 */
@Entity
@Table(name = "loans")
public class Loan {

    // --------------- Primary Key ---------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --------------- Relationships ---------------

    /**
     * The member who borrowed the book.
     * @ManyToOne LAZY: One user can have many loans. Load User only when needed.
     * member_id is the FK column in the loans table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @NotNull(message = "Member is required")
    private User member;

    /**
     * The specific physical copy that was borrowed.
     * @ManyToOne LAZY: One inventory copy can have many loans (over time, not concurrently).
     * inventory_id is the FK column in the loans table.
     *
     * IMPORTANT: A physical copy should have at most ONE active loan at any time.
     * This is enforced by the LoanService with pessimistic locking, NOT by a DB constraint
     * (because a unique constraint would prevent historical re-borrowing of the same copy).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    @NotNull(message = "Inventory copy is required")
    private BookInventory inventory;

    // --------------- Loan Dates ---------------

    /**
     * Date the book was borrowed. Set when loan is created. Never changes.
     */
    @NotNull(message = "Borrow date is required")
    @Column(nullable = false)
    private LocalDate borrowDate;

    /**
     * Date the book is due to be returned.
     * Calculated as: borrowDate + LOAN_PERIOD_DAYS (default: 14 days).
     * Set when loan is created. Never changes (no renewals in this implementation).
     */
    @NotNull(message = "Due date is required")
    @Column(nullable = false)
    private LocalDate dueDate;

    /**
     * Date the book was actually returned. Null until the member returns it.
     * Set by LoanService.returnBook(). Cannot be set by a client API.
     */
    @Column
    private LocalDate returnDate; // nullable - null means not yet returned

    // --------------- Status and Fine ---------------

    /**
     * Current status of this loan.
     * BORROWED -> book is currently with the member.
     * RETURNED -> book has been returned (returnDate is set).
     * OVERDUE  -> book was not returned by dueDate; a fine may apply.
     *
     * Transitions:
     *   BORROWED -> RETURNED (on return)
     *   BORROWED -> OVERDUE  (when dueDate passes without return)
     *   OVERDUE  -> RETURNED (on late return)
     */
    @NotNull(message = "Loan status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.BORROWED; // Default when loan is created

    /**
     * Fine amount charged for overdue return. BigDecimal is used for monetary values
     * to avoid floating-point precision errors (never use double/float for money).
     * Starts at 0.00. Only updated by server-side logic, never by clients.
     * Calculation: (days overdue) × (daily fine rate, e.g., ₹2.00/day).
     */
    @PositiveOrZero(message = "Fine amount cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    // --------------- Timestamp ---------------

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Ensure defaults are set even if caller forgot
        if (this.status == null) {
            this.status = LoanStatus.BORROWED;
        }
        if (this.fineAmount == null) {
            this.fineAmount = BigDecimal.ZERO;
        }
    }

    // --------------- Constructors ---------------

    public Loan() {}

    public Loan(User member, BookInventory inventory, LocalDate borrowDate, LocalDate dueDate) {
        this.member = member;
        this.inventory = inventory;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = LoanStatus.BORROWED;
        this.fineAmount = BigDecimal.ZERO;
    }

    // --------------- Getters and Setters ---------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getMember() { return member; }
    public void setMember(User member) { this.member = member; }

    public BookInventory getInventory() { return inventory; }
    public void setInventory(BookInventory inventory) { this.inventory = inventory; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", memberId=" + (member != null ? member.getId() : null) +
               ", inventoryId=" + (inventory != null ? inventory.getId() : null) +
               ", status=" + status + ", dueDate=" + dueDate + "}";
    }
}
