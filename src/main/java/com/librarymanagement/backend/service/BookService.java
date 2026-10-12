package com.librarymanagement.backend.service;

import com.librarymanagement.backend.dto.BookAvailabilityResponse;
import com.librarymanagement.backend.dto.BookRequest;
import com.librarymanagement.backend.dto.BookResponse;
import com.librarymanagement.backend.entity.Book;
import com.librarymanagement.backend.entity.BookStatus;
import com.librarymanagement.backend.repository.BookInventoryRepository;
import com.librarymanagement.backend.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookInventoryRepository bookInventoryRepository;

    public BookService(BookRepository bookRepository, BookInventoryRepository bookInventoryRepository) {
        this.bookRepository = bookRepository;
        this.bookInventoryRepository = bookInventoryRepository;
    }

    public Page<BookResponse> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(BookResponse::new);
    }

    public Page<BookResponse> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.searchBooks(keyword, pageable).map(BookResponse::new);
    }

    public Page<BookResponse> getBooksByCategory(String category, Pageable pageable) {
        return bookRepository.findByCategoryIgnoreCase(category, pageable).map(BookResponse::new);
    }

    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book", id));
        return new BookResponse(book);
    }

    public BookAvailabilityResponse getBookAvailability(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book", id));
        
        long totalCopies = bookInventoryRepository.findByBookId(id).size();
        long availableCopies = bookInventoryRepository.countByBookIdAndStatus(id, BookStatus.AVAILABLE);

        return new BookAvailabilityResponse(id, totalCopies, availableCopies);
    }

    public BookResponse addBook(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new com.librarymanagement.backend.exception.DuplicateResourceException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        Book book = new Book();
        updateBookFromRequest(book, request);
        return new BookResponse(bookRepository.save(book));
    }

    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book", id));

        // Check if ISBN is being changed and if it already exists
        if (!book.getIsbn().equals(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new com.librarymanagement.backend.exception.DuplicateResourceException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        updateBookFromRequest(book, request);
        return new BookResponse(bookRepository.save(book));
    }

    public void changeBookStatus(Long id, BookStatus status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book", id));
        book.setStatus(status);
        bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book", id));
        
        long totalCopies = bookInventoryRepository.findByBookId(id).size();
        if (totalCopies > 0) {
            throw new com.librarymanagement.backend.exception.BadRequestException("Cannot delete book with existing physical copies");
        }
        
        bookRepository.delete(book);
    }

    private void updateBookFromRequest(Book book, BookRequest request) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
        book.setPublisher(request.getPublisher());
        book.setPublicationDate(request.getPublicationDate());
        book.setCategory(request.getCategory());
        book.setStatus(request.getStatus());
    }
}
