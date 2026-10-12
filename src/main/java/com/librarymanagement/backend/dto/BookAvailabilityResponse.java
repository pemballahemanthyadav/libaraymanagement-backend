package com.librarymanagement.backend.dto;

public class BookAvailabilityResponse {

    private Long bookId;
    private long totalCopies;
    private long availableCopies;

    public BookAvailabilityResponse() {
    }

    public BookAvailabilityResponse(Long bookId, long totalCopies, long availableCopies) {
        this.bookId = bookId;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public long getTotalCopies() { return totalCopies; }
    public void setTotalCopies(long totalCopies) { this.totalCopies = totalCopies; }

    public long getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(long availableCopies) { this.availableCopies = availableCopies; }
}
