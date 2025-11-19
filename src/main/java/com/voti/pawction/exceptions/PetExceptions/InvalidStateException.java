package com.voti.pawction.exceptions.PetExceptions;


public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}