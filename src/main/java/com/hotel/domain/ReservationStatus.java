package com.hotel.domain;

/**
 * Represents the lifecycle states a {@link Reservation} may occupy,
 * from creation through settlement or cancellation.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
