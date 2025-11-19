package com.voti.pawction.exceptions.PetExceptions;


public class PetNotFoundException extends RuntimeException {
    public PetNotFoundException(String message) {
        super(message);
    }
}