package com.hotel.exception;

/**
 * Thrown when a lookup or mutation operation is performed using a booking ID
 * that does not correspond to any known reservation in the repository.
 */
public class InvalidBookingIdException extends Exception {

    private final String bookingId;

    /**
     * @param bookingId the unrecognised booking reference supplied by the caller
     */
    public InvalidBookingIdException(String bookingId) {
        super("No reservation found for booking ID: " + bookingId);
        this.bookingId = bookingId;
    }

    /** @return The booking ID that could not be resolved. */
    public String getBookingId() { return bookingId; }
}
