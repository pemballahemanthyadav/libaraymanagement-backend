package com.librarymanagement.backend.dto;

import com.librarymanagement.backend.entity.Book;
import com.librarymanagement.backend.entity.BookStatus;

import java.time.LocalDate;

public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String description;
    private String publisher;
    private LocalDate publicationDate;
    private String category;
    private BookStatus status;
    private Long totalCopies;
    private Long availableCopies;

    public BookResponse() {
    }

    public BookResponse(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.isbn = book.getIsbn();
        this.description = book.getDescription();
        this.publisher = book.getPublisher();
        this.publicationDate = book.getPublicationDate();
        this.category = book.getCategory();
        this.status = book.getStatus();
    }

    public BookResponse(Book book, Long totalCopies, Long availableCopies) {
        this(book);
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    // Getters and Setters

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

    public Long getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Long totalCopies) { this.totalCopies = totalCopies; }

    public Long getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Long availableCopies) { this.availableCopies = availableCopies; }
}
