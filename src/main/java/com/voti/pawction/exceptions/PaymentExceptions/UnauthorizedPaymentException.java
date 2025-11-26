package com.voti.pawction.exceptions.PaymentExceptions;

public class UnauthorizedPaymentException extends RuntimeException {
    public UnauthorizedPaymentException(String message) {
        super(message);
    }
}
