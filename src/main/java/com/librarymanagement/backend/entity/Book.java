package com.librarymanagement.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Book Entity - represents a book TITLE in the library catalog.
 *
 * This entity stores bibliographic information (title, author, ISBN, publisher, etc.).
 * It does NOT represent a physical copy. Physical copies are tracked in BookInventory.
 *
 * Design Decision - Title vs. Copy separation:
 *   A single Book record can have many BookInventory records (physical copies).
 *   This allows the library to own multiple copies of the same title and track
 *   which specific copy is borrowed by which member.
 *   Example: "Clean Code" (Book) might have 3 copies (3 BookInventory records).
 *
 * Maps to the "books" table in MySQL.
 */
@Entity
@Table(name = "books")
public class Book {

    // --------------- Primary Key ---------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --------------- Required Fields ---------------

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String author;

    /**
     * International Standard Book Number. Must be unique across all books.
     * Supports both ISBN-10 and ISBN-13 formats (length 10-17 with hyphens).
     */
    @NotBlank(message = "ISBN is required")
    @Size(max = 20, message = "ISBN must not exceed 20 characters")
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    /**
     * Optional long description of the book.
     * @Column(columnDefinition = "TEXT") uses MySQL TEXT type (up to 65,535 chars)
     * instead of VARCHAR to accommodate long book descriptions.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 255, message = "Publisher must not exceed 255 characters")
    @Column(length = 255)
    private String publisher;

    /**
     * Publication date as a date only (no time component needed).
     */
    private LocalDate publicationDate;

    /**
     * Category/genre of the book (e.g., "Science Fiction", "Computer Science").
     * Used for searching and filtering the catalog.
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Column(length = 100)
    private String category;

    /**
     * Overall status of the book title in the catalog.
     * AVAILABLE  -> Title is active and copies can be borrowed.
     * UNAVAILABLE -> Title is temporarily inactive.
     * ARCHIVED   -> Title is retired from the catalog.
     *
     * NOTE: This is the title-level status. Actual borrowing availability is
     * determined by checking individual BookInventory copy statuses, not just this field.
     */
    @NotNull(message = "Book status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.AVAILABLE; // Default to AVAILABLE when created

    // --------------- Timestamps ---------------

    /**
     * Automatically set when the book is first added to the catalog.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated every time the book record is modified.
     * @PreUpdate: JPA calls this method automatically before every UPDATE operation.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @PrePersist: called before INSERT - sets both timestamps.
     * @PreUpdate:  called before UPDATE - updates only updatedAt.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --------------- Constructors ---------------

    public Book() {}

    public Book(String title, String author, String isbn, BookStatus status) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.status = status;
    }

    // --------------- Getters and Setters ---------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', author='" + author +
               "', isbn='" + isbn + "', status=" + status + "}";
    }
}
