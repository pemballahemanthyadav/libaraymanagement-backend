package com.librarymanagement.backend.controller;

import com.librarymanagement.backend.dto.BookAvailabilityResponse;
import com.librarymanagement.backend.dto.BookInventoryResponse;
import com.librarymanagement.backend.dto.BookResponse;
import com.librarymanagement.backend.service.BookInventoryService;
import com.librarymanagement.backend.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class PublicBookController {

    private final BookService bookService;
    private final BookInventoryService bookInventoryService;

    public PublicBookController(BookService bookService, BookInventoryService bookInventoryService) {
        this.bookService = bookService;
        this.bookInventoryService = bookInventoryService;
    }

    @GetMapping
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getAllBooks(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.searchBooks(keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<BookAvailabilityResponse> checkAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookAvailability(id));
    }

    @GetMapping("/{id}/copies")
    public ResponseEntity<List<BookInventoryResponse>> getBookCopies(@PathVariable Long id) {
        return ResponseEntity.ok(bookInventoryService.getCopiesForBook(id));
    }
}
