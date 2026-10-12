package com.librarymanagement.backend.repository;

import com.librarymanagement.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - Spring Data JPA repository for the User entity.
 *
 * JpaRepository<User, Long> provides:
 *   save(), findById(), findAll(), delete(), existsById(), count(), etc.
 *   We get all standard CRUD operations for FREE without writing any SQL.
 *
 * Spring Data auto-generates queries from method names.
 *   findByEmail -> SELECT * FROM users WHERE email = ?
 *   existsByEmail -> SELECT COUNT(*) > 0 FROM users WHERE email = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used during login to look up the user and verify their password.
     *
     * Returns Optional<User> so the caller must explicitly handle the case
     * where no user with that email exists (avoids NullPointerException).
     *
     * @param email the email address to search for
     * @return Optional containing the User if found, empty Optional otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given email already exists.
     * Used during registration to prevent duplicate accounts.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
