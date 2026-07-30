package com.hotel.exception;

/**
 * Thrown when the payment simulation fails to process a transaction,
 * for example due to a declined card or insufficient simulated funds.
 */
public class PaymentException extends Exception {

    /**
     * @param message a human-readable description of the payment failure reason
     */
    public PaymentException(String message) {
        super(message);
    }

    /**
     * @param message a human-readable description of the payment failure reason
     * @param cause   the underlying cause if wrapping a lower-level exception
     */
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
