package com.librarymanagement.backend.service;

import com.librarymanagement.backend.dto.BookInventoryRequest;
import com.librarymanagement.backend.dto.BookInventoryResponse;
import com.librarymanagement.backend.entity.Book;
import com.librarymanagement.backend.entity.BookInventory;
import com.librarymanagement.backend.entity.BookStatus;
import com.librarymanagement.backend.repository.BookInventoryRepository;
import com.librarymanagement.backend.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookInventoryService {

    private final BookInventoryRepository bookInventoryRepository;
    private final BookRepository bookRepository;

    public BookInventoryService(BookInventoryRepository bookInventoryRepository, BookRepository bookRepository) {
        this.bookInventoryRepository = bookInventoryRepository;
        this.bookRepository = bookRepository;
    }

    public List<BookInventoryResponse> getCopiesForBook(Long bookId) {
        return bookInventoryRepository.findByBookId(bookId).stream()
                .map(BookInventoryResponse::new)
                .collect(Collectors.toList());
    }

    public BookInventoryResponse addCopy(BookInventoryRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book", request.getBookId()));

        if (bookInventoryRepository.existsByAccessionNumber(request.getAccessionNumber())) {
            throw new com.librarymanagement.backend.exception.DuplicateResourceException("Accession number already exists: " + request.getAccessionNumber());
        }

        BookInventory inventory = new BookInventory();
        inventory.setBook(book);
        inventory.setAccessionNumber(request.getAccessionNumber());
        inventory.setShelfLocation(request.getShelfLocation());
        inventory.setStatus(request.getStatus());

        return new BookInventoryResponse(bookInventoryRepository.save(inventory));
    }

    public void changeCopyStatus(Long copyId, BookStatus status) {
        BookInventory inventory = bookInventoryRepository.findById(copyId)
                .orElseThrow(() -> new com.librarymanagement.backend.exception.ResourceNotFoundException("Book copy", copyId));
        inventory.setStatus(status);
        bookInventoryRepository.save(inventory);
    }
}
