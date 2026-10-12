package com.librarymanagement.backend.controller;

import com.librarymanagement.backend.dto.BookRequest;
import com.librarymanagement.backend.dto.BookResponse;
import com.librarymanagement.backend.entity.BookStatus;
import com.librarymanagement.backend.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/books")
public class AdminBookController {

    private final BookService bookService;

    public AdminBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> addBook(@Valid @RequestBody BookRequest request) {
        return new ResponseEntity<>(bookService.addBook(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getAllBooks(PageRequest.of(page, size)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id, 
            @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeBookStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        BookStatus status = BookStatus.valueOf(request.get("status").toUpperCase());
        bookService.changeBookStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
