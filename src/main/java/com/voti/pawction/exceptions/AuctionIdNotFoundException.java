package com.voti.pawction.exceptions;

public class AuctionIdNotFoundException extends RuntimeException {
    public AuctionIdNotFoundException(String message) {
        super(message);
    }
}
