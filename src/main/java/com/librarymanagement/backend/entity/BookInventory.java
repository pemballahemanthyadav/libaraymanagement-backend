package com.librarymanagement.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * BookInventory Entity - represents a PHYSICAL COPY of a book owned by the library.
 *
 * Why a separate entity?
 *   A library may own multiple physical copies of the same title.
 *   Each copy gets its own row here with a unique accession number and shelf location.
 *   Loans are linked to a specific copy (BookInventory), not to the title (Book).
 *   This ensures the system knows exactly which physical book is with which member.
 *
 * Example:
 *   Book: "Clean Code" (id=1)
 *   BookInventory: Copy 1 - ACC001 (shelf A3, AVAILABLE)
 *   BookInventory: Copy 2 - ACC002 (shelf A3, BORROWED - loan record exists)
 *   BookInventory: Copy 3 - ACC003 (shelf A4, ARCHIVED)
 *
 * Availability logic:
 *   A copy can be borrowed if and only if:
 *     1. BookInventory.status == AVAILABLE (this copy is not archived or marked unavailable)
 *     2. No active Loan record exists for this copy (no row with status = BORROWED or OVERDUE)
 *   Condition 2 is enforced by the LoanService using a locking database query.
 *
 * Duplicate-borrow prevention strategy (important design decision):
 *   A simple UNIQUE constraint on (member_id, inventory_id) on the Loan table would PREVENT
 *   a member from ever borrowing the same copy again (even after returning it) — so we DON'T use that.
 *
 *   Instead, we use TWO complementary mechanisms:
 *
 *   1. Service-layer transactional check with pessimistic locking:
 *      In LoanService.borrowBook(), we acquire a PESSIMISTIC_WRITE lock on the BookInventory row
 *      before checking if any active loan exists. This prevents two concurrent requests from
 *      both reading "no active loan" and both proceeding to create duplicate loans.
 *      (@Lock(LockModeType.PESSIMISTIC_WRITE) in BookInventoryRepository)
 *
 *   2. BookInventory.status field:
 *      When a loan is created, the copy's status is set to UNAVAILABLE (it's physically checked out).
 *      When the book is returned, the status is set back to AVAILABLE.
 *      The service checks this status before allowing a borrow operation.
 *      This provides a second layer of defense even without the lock.
 *
 *   Together these prevent the "double-borrow" race condition without restricting historical records.
 *
 * Maps to the "book_inventory" table in MySQL.
 */
@Entity
@Table(name = "book_inventory")
public class BookInventory {

    // --------------- Primary Key ---------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --------------- Relationship to Book ---------------

    /**
     * The book title this physical copy belongs to.
     *
     * @ManyToOne: Many copies can belong to one book.
     * fetch = FetchType.LAZY: The Book is only loaded from DB when we call getBook().
     *   This avoids loading the entire Book object every time we load an inventory record —
     *   important for performance, especially in lists.
     * @JoinColumn(name = "book_id"): Creates a foreign key column "book_id" in book_inventory table.
     * nullable = false: Every physical copy must belong to a book title.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @NotNull(message = "Book reference is required")
    private Book book;

    // --------------- Physical Copy Details ---------------

    /**
     * Unique accession number assigned to each physical copy.
     * Typically stamped inside the book cover (e.g., "ACC-2024-001").
     * Used to identify the specific physical copy being borrowed.
     */
    @NotBlank(message = "Accession number is required")
    @Size(max = 50, message = "Accession number must not exceed 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String accessionNumber;

    /**
     * Where the copy is physically located in the library.
     * Example: "Shelf A3", "Room 2 - Shelf 5"
     */
    @Size(max = 100, message = "Shelf location must not exceed 100 characters")
    @Column(length = 100)
    private String shelfLocation;

    /**
     * Status of this specific physical copy.
     * AVAILABLE   -> Can be borrowed right now.
     * UNAVAILABLE -> Currently checked out (a Loan exists) or temporarily unavailable.
     * ARCHIVED    -> Retired (damaged, lost, etc.). Cannot be borrowed. Historical loans preserved.
     *
     * This is updated by LoanService when a book is borrowed (-> UNAVAILABLE)
     * and when returned (-> AVAILABLE).
     */
    @NotNull(message = "Copy status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.AVAILABLE;

    // --------------- Timestamp ---------------

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --------------- Constructors ---------------

    public BookInventory() {}

    public BookInventory(Book book, String accessionNumber, String shelfLocation) {
        this.book = book;
        this.accessionNumber = accessionNumber;
        this.shelfLocation = shelfLocation;
        this.status = BookStatus.AVAILABLE;
    }

    // --------------- Getters and Setters ---------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "BookInventory{id=" + id + ", accessionNumber='" + accessionNumber +
               "', status=" + status + ", shelfLocation='" + shelfLocation + "'}";
    }
}
