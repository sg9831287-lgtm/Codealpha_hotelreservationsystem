package com.hotel.domain;

/**
 * Enumerates the possible settlement states of a payment transaction
 * attached to a {@link Reservation}.
 */
public enum PaymentStatus {
    UNPAID,
    PAID,
    REFUNDED
}
