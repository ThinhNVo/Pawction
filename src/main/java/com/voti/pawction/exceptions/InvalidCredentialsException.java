package com.voti.pawction.exceptions;

/**
 * Exception thrown when authentication credentials are invalid
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

