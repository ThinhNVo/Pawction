package com.voti.pawction.exceptions.AuctionExceptions;

public class InvalidAuctionException extends RuntimeException {
    public InvalidAuctionException(String message) {
        super(message);
    }
}
