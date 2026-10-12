package com.librarymanagement.backend.entity;

/**
 * BookStatus enum - represents the current state of a Book (title record) or a physical BookInventory copy.
 *
 * AVAILABLE   -> The book/copy can be borrowed by members.
 * UNAVAILABLE -> Temporarily cannot be borrowed (e.g., under repair, reserved).
 * ARCHIVED    -> Permanently retired from active circulation; cannot be borrowed.
 *               Historical loan records are preserved even for archived copies.
 *
 * Design decision: BookStatus is used on BOTH the Book entity (overall title status)
 * and the BookInventory entity (individual physical copy status).
 *
 * - Book.status = AVAILABLE means the title itself is active in the catalog.
 * - BookInventory.status = AVAILABLE means that specific physical copy can be borrowed.
 *
 * Borrowing availability is determined by checking BookInventory.status = AVAILABLE
 * AND no active loan exists for that copy. The Book.status alone is NOT sufficient
 * to determine if a copy can be borrowed — we check the inventory copy's status.
 */
public enum BookStatus {
    AVAILABLE,
    BORROWED,
    UNAVAILABLE,
    ARCHIVED
}
