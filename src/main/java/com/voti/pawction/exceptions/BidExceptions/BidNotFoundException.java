package com.voti.pawction.exceptions.BidExceptions;

public class BidNotFoundException extends RuntimeException {
    public BidNotFoundException(String message) {
        super(message);
    }
}
