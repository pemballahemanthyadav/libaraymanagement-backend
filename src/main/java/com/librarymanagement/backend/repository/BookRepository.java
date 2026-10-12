package com.librarymanagement.backend.repository;

import com.librarymanagement.backend.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * BookRepository - Spring Data JPA repository for the Book entity.
 *
 * Provides standard CRUD operations plus custom search queries.
 *
 * Pagination support: methods accepting Pageable return Page<Book> which includes:
 *   - The list of books for the current page
 *   - Total number of books (for calculating total pages)
 *   - Current page number and size
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Find a book by its ISBN.
     * Used to verify ISBN uniqueness and to look up a specific title.
     *
     * @param isbn the ISBN to search for
     * @return Optional<Book> - empty if no book with this ISBN exists
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Check if a book with the given ISBN already exists.
     * Used when adding a new book to prevent duplicate ISBNs.
     *
     * @param isbn the ISBN to check
     * @return true if a book with this ISBN exists
     */
    boolean existsByIsbn(String isbn);

    /**
     * Full-text search across title, author, ISBN, and category.
     *
     * Uses JPQL (Java Persistence Query Language) - a SQL-like language that works
     * with entity class names and field names instead of table/column names.
     *
     * LOWER() makes the search case-insensitive.
     * LIKE '%keyword%' matches the keyword anywhere in the field value.
     * :keyword is a named parameter bound from the @Param annotation.
     *
     * Pageable allows the caller to request a specific page and sort order.
     * The query returns a Page<Book> with pagination metadata.
     *
     * @param keyword the search term (partial match, case-insensitive)
     * @param pageable pagination and sorting settings
     * @return Page of matching books
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find all books in a specific category, with pagination.
     * Case-insensitive exact match on the category field.
     *
     * @param category the category to filter by
     * @param pageable pagination settings
     * @return Page of books in the given category
     */
    Page<Book> findByCategoryIgnoreCase(String category, Pageable pageable);

    /**
     * Find all books with a specific status, with pagination.
     * Used by admins to filter catalog by status (AVAILABLE, ARCHIVED, etc.).
     *
     * @param status the BookStatus to filter by
     * @param pageable pagination settings
     * @return Page of books with the given status
     */
    Page<Book> findByStatus(com.librarymanagement.backend.entity.BookStatus status, Pageable pageable);
}
