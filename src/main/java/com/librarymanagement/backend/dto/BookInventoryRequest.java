package com.librarymanagement.backend.dto;

import com.librarymanagement.backend.entity.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookInventoryRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotBlank(message = "Accession number is required")
    @Size(max = 50)
    private String accessionNumber;

    @Size(max = 100)
    private String shelfLocation;

    @NotNull(message = "Status is required")
    private BookStatus status = BookStatus.AVAILABLE;

    // Getters and Setters
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }
}
