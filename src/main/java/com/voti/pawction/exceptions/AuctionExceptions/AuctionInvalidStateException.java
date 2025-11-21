package com.voti.pawction.exceptions.AuctionExceptions;

public class AuctionInvalidStateException extends RuntimeException {
    public AuctionInvalidStateException(String message) {
        super(message);
    }
}
