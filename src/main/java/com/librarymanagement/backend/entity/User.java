package com.librarymanagement.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * User Entity - represents a person registered in the Library Management System.
 *
 * Maps to the "users" table in MySQL.
 * Two roles exist: ADMIN (librarian) and MEMBER (library user).
 *
 * IMPORTANT: The password field stores a BCrypt hash, never plain text.
 *            The password is NEVER exposed through any API response (see UserResponse DTO).
 *
 * JPA Annotations explained:
 *   @Entity         -> Marks this class as a JPA entity (maps to a DB table).
 *   @Table          -> Specifies the exact table name "users" (avoids using reserved word "user").
 *   @Id             -> Marks the primary key field.
 *   @GeneratedValue -> Auto-increments the ID using the DB's identity strategy.
 *   @Column         -> Configures column properties (nullable, unique, length).
 *   @Enumerated     -> Stores the enum as its String name in the DB, not as an integer index.
 *   @PrePersist     -> Runs a method automatically before the entity is first saved to DB.
 */
@Entity
@Table(name = "users")
public class User {

    // --------------- Primary Key ---------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT in MySQL
    private Long id;

    // --------------- Required Fields ---------------

    /**
     * Full name of the user. Required, max 100 characters.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Email address. Used as the login identifier. Must be unique across all users.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * BCrypt-hashed password. Never stored as plain text. Never exposed via API.
     */
    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    /**
     * Optional phone number for contact purposes.
     */
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Column(length = 20)
    private String phone;

    /**
     * Role determines access level: ADMIN or MEMBER.
     * Stored as the String "ADMIN" or "MEMBER" in the DB.
     * NEVER set to ADMIN through public registration.
     */
    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)    // Store as "ADMIN" or "MEMBER", not 0 or 1
    @Column(nullable = false, length = 20)
    private Role role;

    // --------------- Timestamps ---------------

    /**
     * Automatically set to the current timestamp when the record is first created.
     * @Column(updatable = false) ensures this is never changed after creation.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * @PrePersist: JPA calls this method automatically just before inserting a new record.
     * This ensures createdAt is always set without the caller having to set it manually.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --------------- Constructors ---------------

    /**
     * No-args constructor required by JPA (Hibernate needs to instantiate entities via reflection).
     */
    public User() {}

    /**
     * Convenience constructor for creating a new user programmatically.
     */
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // --------------- Getters and Setters ---------------
    // Explicit getters/setters are used instead of Lombok so beginners can see every method.

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        // IMPORTANT: Never include password in toString() to avoid accidental logging.
        return "User{id=" + id + ", name='" + name + "', email='" + email +
               "', role=" + role + ", createdAt=" + createdAt + "}";
    }
}
