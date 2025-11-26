package com.voti.pawction.exceptions.PaymentExceptions;

public class InvalidPaymentException extends RuntimeException {
  public InvalidPaymentException(String message) {
    super(message);
  }
}
