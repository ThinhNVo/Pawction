package com.voti.pawction.exceptions.AuctionExceptions;

public class AuctionNotFoundException extends RuntimeException {
    public AuctionNotFoundException(String message) {
        super(message);
    }
}
