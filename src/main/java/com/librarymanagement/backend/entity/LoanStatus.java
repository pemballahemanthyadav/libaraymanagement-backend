package com.librarymanagement.backend.entity;

/**
 * LoanStatus enum - represents the lifecycle state of a borrowing transaction (Loan).
 *
 * BORROWED -> The book has been checked out and is currently with the member.
 *             The returnDate on the Loan is null.
 * RETURNED -> The member has returned the book. returnDate is set.
 *             The associated inventory copy's status is set back to AVAILABLE.
 * OVERDUE  -> The book was not returned by its dueDate.
 *             Status is updated by the LoanService (either on each request or by a scheduled job).
 *             A fine (fineAmount) may be calculated when status transitions to OVERDUE.
 *
 * Historical records are NEVER deleted. Once a loan is RETURNED or OVERDUE,
 * the record is preserved for audit, statistics, and borrowing history.
 */
public enum LoanStatus {
    BORROWED,
    RETURNED,
    OVERDUE
}
