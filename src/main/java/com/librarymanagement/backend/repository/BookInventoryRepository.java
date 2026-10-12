package com.librarymanagement.backend.repository;

import com.librarymanagement.backend.entity.BookInventory;
import com.librarymanagement.backend.entity.BookStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BookInventoryRepository - Spring Data JPA repository for BookInventory (physical copies).
 *
 * The most important method here is findByIdWithLock(), which uses PESSIMISTIC_WRITE locking.
 * This is the core mechanism that prevents two concurrent borrow requests from both
 * succeeding for the same physical copy.
 *
 * How the pessimistic lock works:
 *   When LoanService calls findByIdWithLock(copyId), MySQL executes:
 *     SELECT * FROM book_inventory WHERE id = ? FOR UPDATE
 *   The "FOR UPDATE" clause locks that specific row in the database.
 *   Any other transaction trying to read or write that row must WAIT until this
 *   transaction commits or rolls back.
 *   This ensures only one borrow request processes at a time for a given copy.
 *   The entire lock acquisition and loan creation happens inside a @Transactional method.
 */
@Repository
public interface BookInventoryRepository extends JpaRepository<BookInventory, Long> {

    /**
     * Find all physical copies belonging to a specific book.
     * Used by admins to view all copies of a title, and by members to see availability.
     *
     * @param bookId the ID of the book title
     * @return list of all inventory copies for that book
     */
    @EntityGraph(attributePaths = {"book"})
    List<BookInventory> findByBookId(Long bookId);

    /**
     * Find only the AVAILABLE copies of a specific book.
     * A copy is "available" if its status is AVAILABLE — but note that the LoanService
     * also checks that no active loan exists for the copy (double-check for safety).
     *
     * @param bookId the ID of the book title
     * @param status typically BookStatus.AVAILABLE
     * @return list of copies with the given status
     */
    @EntityGraph(attributePaths = {"book"})
    List<BookInventory> findByBookIdAndStatus(Long bookId, BookStatus status);

    /**
     * Count the number of available copies for a specific book.
     * Used in BookService to show the member how many copies are currently available.
     *
     * @param bookId the ID of the book title
     * @param status typically BookStatus.AVAILABLE
     * @return count of copies with that status
     */
    long countByBookIdAndStatus(Long bookId, BookStatus status);

    /**
     * Find a physical copy by its accession number.
     * Accession numbers are unique (like a barcode) stamped on each physical book.
     *
     * @param accessionNumber the unique accession number
     * @return Optional<BookInventory> - empty if not found
     */
    Optional<BookInventory> findByAccessionNumber(String accessionNumber);

    /**
     * Check if an accession number already exists.
     * Used when adding a new physical copy to prevent duplicate accession numbers.
     *
     * @param accessionNumber the accession number to check
     * @return true if a copy with this accession number already exists
     */
    boolean existsByAccessionNumber(String accessionNumber);

    /**
     * CRITICAL - Pessimistic locking query for borrow operations.
     *
     * @Lock(LockModeType.PESSIMISTIC_WRITE) tells JPA to add "FOR UPDATE" to the SQL query.
     * This acquires an exclusive row-level lock on the BookInventory record.
     *
     * Usage in LoanService.borrowBook():
     *   1. Start a @Transactional method.
     *   2. Call this method to get the inventory copy WITH a database lock.
     *   3. Check if the copy is AVAILABLE and has no active loans.
     *   4. If checks pass, create the Loan and update inventory status.
     *   5. Commit the transaction — which releases the lock.
     *
     * If two requests arrive simultaneously for the same copy:
     *   - Request A gets the lock first, completes the borrow, commits (releases lock).
     *   - Request B gets the lock after A commits, finds the copy is now UNAVAILABLE, rejects.
     *
     * IMPORTANT: This method must ONLY be called inside a @Transactional context,
     * otherwise the lock is released immediately after the SELECT (not useful).
     *
     * @param id the inventory copy's primary key
     * @return Optional<BookInventory> with the row locked for update
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bi FROM BookInventory bi WHERE bi.id = :id")
    Optional<BookInventory> findByIdWithLock(@Param("id") Long id);
}
