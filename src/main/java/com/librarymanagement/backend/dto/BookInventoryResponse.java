package com.librarymanagement.backend.dto;

import com.librarymanagement.backend.entity.BookInventory;
import com.librarymanagement.backend.entity.BookStatus;

import java.time.LocalDateTime;

public class BookInventoryResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String accessionNumber;
    private String shelfLocation;
    private BookStatus status;
    private LocalDateTime createdAt;

    public BookInventoryResponse() {
    }

    public BookInventoryResponse(BookInventory inventory) {
        this.id = inventory.getId();
        this.bookId = inventory.getBook().getId();
        this.bookTitle = inventory.getBook().getTitle();
        this.accessionNumber = inventory.getAccessionNumber();
        this.shelfLocation = inventory.getShelfLocation();
        this.status = inventory.getStatus();
        this.createdAt = inventory.getCreatedAt();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
