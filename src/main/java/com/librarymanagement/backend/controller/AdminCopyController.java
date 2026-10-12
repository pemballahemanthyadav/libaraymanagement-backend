package com.librarymanagement.backend.controller;

import com.librarymanagement.backend.dto.BookInventoryRequest;
import com.librarymanagement.backend.dto.BookInventoryResponse;
import com.librarymanagement.backend.entity.BookStatus;
import com.librarymanagement.backend.service.BookInventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminCopyController {

    private final BookInventoryService bookInventoryService;

    public AdminCopyController(BookInventoryService bookInventoryService) {
        this.bookInventoryService = bookInventoryService;
    }

    @PostMapping("/books/{bookId}/copies")
    public ResponseEntity<BookInventoryResponse> addCopy(
            @PathVariable Long bookId, 
            @Valid @RequestBody BookInventoryRequest request) {
        // Ensure the path variable matches the body if provided, or set it
        request.setBookId(bookId);
        return new ResponseEntity<>(bookInventoryService.addCopy(request), HttpStatus.CREATED);
    }

    @GetMapping("/books/{bookId}/copies")
    public ResponseEntity<List<BookInventoryResponse>> getCopiesForBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookInventoryService.getCopiesForBook(bookId));
    }

    @PatchMapping("/copies/{copyId}/status")
    public ResponseEntity<Void> changeCopyStatus(
            @PathVariable Long copyId, 
            @RequestBody Map<String, String> request) {
        BookStatus status = BookStatus.valueOf(request.get("status").toUpperCase());
        bookInventoryService.changeCopyStatus(copyId, status);
        return ResponseEntity.noContent().build();
    }
}
