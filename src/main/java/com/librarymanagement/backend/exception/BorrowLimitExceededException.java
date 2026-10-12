package com.librarymanagement.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a member attempts to borrow more books than the allowed maximum.
 * Maps to HTTP 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BorrowLimitExceededException extends RuntimeException {

    public BorrowLimitExceededException(String message) {
        super(message);
    }
}
