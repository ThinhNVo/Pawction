package com.voti.pawction.exceptions;

/**
 * Exception thrown when user's email already exists
 */
public class UserEmailExistsException extends RuntimeException {
    public UserEmailExistsException(String message) {
        super(message);
    }
}
