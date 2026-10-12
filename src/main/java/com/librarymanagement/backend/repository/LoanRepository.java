package com.librarymanagement.backend.repository;

import com.librarymanagement.backend.entity.Loan;
import com.librarymanagement.backend.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * LoanRepository - Spring Data JPA repository for Loan (borrowing transaction) records.
 *
 * Key queries explained:
 *
 * "Active loan" definition used throughout this system:
 *   A loan is "active" if its status is BORROWED or OVERDUE.
 *   RETURNED loans are historical records, not active.
 *
 * findActiveLoansForInventory: Used by LoanService to check if a specific physical copy
 *   is currently borrowed before allowing another member to borrow it.
 *   This query is called AFTER acquiring the pessimistic lock in BookInventoryRepository.
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // =================== MEMBER QUERIES ===================

    /**
     * Find all loans for a specific member (all history, any status).
     * Used in the member's borrowing history page.
     * Returns a Page for pagination support.
     *
     * @param memberId the member's user ID
     * @param pageable pagination and sort settings
     * @return Page of all loans for this member
     */
    Page<Loan> findByMemberId(Long memberId, Pageable pageable);

    /**
     * Find only active loans (BORROWED or OVERDUE) for a member.
     * Used in the "My Borrowed Books" page to show currently checked-out books.
     *
     * @param memberId the member's user ID
     * @return list of active loans for this member
     */
    @Query("SELECT l FROM Loan l WHERE l.member.id = :memberId " +
           "AND l.status IN (com.librarymanagement.backend.entity.LoanStatus.BORROWED, " +
           "                 com.librarymanagement.backend.entity.LoanStatus.OVERDUE)")
    List<Loan> findActiveByMemberId(@Param("memberId") Long memberId);

    /**
     * Count how many active loans a member currently has.
     * Used to enforce the maximum borrow limit (e.g., 5 books at a time).
     *
     * @param memberId the member's user ID
     * @return count of currently active loans for this member
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId " +
           "AND l.status IN (com.librarymanagement.backend.entity.LoanStatus.BORROWED, " +
           "                 com.librarymanagement.backend.entity.LoanStatus.OVERDUE)")
    long countActiveByMemberId(@Param("memberId") Long memberId);

    /**
     * Alias used by LoanService.borrowBook() to enforce borrow limit.
     */
    default long countActiveLoansForMember(Long memberId) {
        return countActiveByMemberId(memberId);
    }

    /**
     * Alias used by LoanService to get all active loans for a member.
     */
    default List<Loan> findActiveLoansForMember(Long memberId) {
        return findActiveByMemberId(memberId);
    }

    /**
     * Find a loan by ID that belongs to a specific member.
     * Used for return operations to ensure a member can only return their own books.
     * Prevents member A from returning member B's loan.
     *
     * @param loanId   the loan ID
     * @param memberId the member's user ID
     * @return Optional<Loan> - empty if the loan doesn't exist or doesn't belong to this member
     */
    Optional<Loan> findByIdAndMemberId(Long loanId, Long memberId);

    // =================== INVENTORY / CONCURRENCY QUERIES ===================

    /**
     * CRITICAL - Check if there is any active loan for a specific inventory copy.
     * Called inside the @Transactional + pessimistic-lock block in LoanService.
     *
     * If this returns a non-empty Optional, the copy is currently borrowed by someone
     * and cannot be borrowed again until returned.
     *
     * Only status BORROWED and OVERDUE count as "active" — RETURNED does not.
     *
     * @param inventoryId the physical copy's ID
     * @return Optional<Loan> - non-empty if an active loan exists for this copy
     */
    @Query("SELECT l FROM Loan l WHERE l.inventory.id = :inventoryId " +
           "AND l.status IN (com.librarymanagement.backend.entity.LoanStatus.BORROWED, " +
           "                 com.librarymanagement.backend.entity.LoanStatus.OVERDUE)")
    Optional<Loan> findActiveLoanByInventoryId(@Param("inventoryId") Long inventoryId);

    /**
     * Returns all active loans for an inventory copy (as a List).
     * Used by LoanService.borrowBook() to double-check before issuing a new loan.
     */
    @Query("SELECT l FROM Loan l WHERE l.inventory.id = :inventoryId " +
           "AND l.status IN (com.librarymanagement.backend.entity.LoanStatus.BORROWED, " +
           "                 com.librarymanagement.backend.entity.LoanStatus.OVERDUE)")
    List<Loan> findActiveLoansForInventory(@Param("inventoryId") Long inventoryId);

    // =================== ADMIN QUERIES ===================

    /**
     * Find all loans in the system, with pagination.
     * Used by admins to see every borrowing transaction across all members.
     * JpaRepository.findAll(Pageable) already provides this — no custom query needed.
     */

    /**
     * Find all loans with a specific status, with pagination.
     * Used by admins to filter loans (e.g., show only OVERDUE loans).
     *
     * @param status   the LoanStatus to filter by
     * @param pageable pagination settings
     * @return Page of loans with the given status
     */
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    /**
     * Find all overdue loans: active loans where dueDate has passed.
     *
     * Uses today's date to find loans that are BORROWED (not yet returned)
     * AND whose dueDate is before today. These are technically overdue.
     * Also includes loans already marked OVERDUE in status.
     *
     * This is used:
     *   1. By admins to view the overdue list.
     *   2. By a service method to update loan statuses to OVERDUE when due date passes.
     *
     * @param today    today's date (passed in so it can be tested with any date)
     * @param pageable pagination settings
     * @return Page of overdue loans
     */
    @Query("SELECT l FROM Loan l WHERE " +
           "(l.status = com.librarymanagement.backend.entity.LoanStatus.BORROWED " +
           " AND l.dueDate < :today) " +
           "OR l.status = com.librarymanagement.backend.entity.LoanStatus.OVERDUE")
    Page<Loan> findOverdueLoans(@Param("today") LocalDate today, Pageable pageable);

    /**
     * Find loans that are BORROWED and past due date, for status update.
     * Used by LoanService to batch-update BORROWED -> OVERDUE for loans past their due date.
     *
     * @param today today's date
     * @return list of loans to mark as OVERDUE
     */
    @Query("SELECT l FROM Loan l WHERE " +
           "l.status = com.librarymanagement.backend.entity.LoanStatus.BORROWED " +
           "AND l.dueDate < :today")
    List<Loan> findBorrowedLoansPassedDueDate(@Param("today") LocalDate today);

    /**
     * Find loans that are BORROWED and overdue (no Pageable) — used by markOverdueLoans().
     */
    @Query("SELECT l FROM Loan l WHERE " +
           "l.status = com.librarymanagement.backend.entity.LoanStatus.BORROWED " +
           "AND l.dueDate < :today")
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    /**
     * Find all loans for a specific inventory copy.
     * Used by admins to view the complete borrowing history of a physical copy.
     *
     * @param inventoryId the inventory copy's ID
     * @return list of all loans for this copy
     */
    List<Loan> findByInventoryId(Long inventoryId);
}
