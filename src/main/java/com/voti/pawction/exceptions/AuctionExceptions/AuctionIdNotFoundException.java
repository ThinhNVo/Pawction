package com.voti.pawction.exceptions.AuctionExceptions;

public class AuctionIdNotFoundException extends RuntimeException {
    public AuctionIdNotFoundException(String message) {
        super(message);
    }
}
