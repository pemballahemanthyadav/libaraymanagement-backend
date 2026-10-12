package com.librarymanagement.backend.entity;

/**
 * Role enum - represents the two types of users in the Library Management System.
 *
 * ADMIN  -> Librarian / Administrator: can manage books, inventory, members, loans.
 * MEMBER -> Regular library user: can browse, borrow, and return books.
 *
 * Stored as a String in the database (e.g., "ADMIN" or "MEMBER") using EnumType.STRING.
 * This is safer than EnumType.ORDINAL because adding new roles won't break existing data.
 *
 * Security note: The ADMIN role is NEVER assignable through public registration.
 * The first admin account must be created via a controlled database setup (see README.md).
 */
public enum Role {
    ADMIN,
    MEMBER
}
