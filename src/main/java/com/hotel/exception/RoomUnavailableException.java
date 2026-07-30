package com.hotel.exception;

/**
 * Thrown when a booking operation targets a room that is currently occupied
 * or otherwise ineligible for reservation.
 */
public class RoomUnavailableException extends Exception {

    private final String roomNumber;

    /**
     * @param roomNumber the identifier of the room that could not be reserved
     */
    public RoomUnavailableException(String roomNumber) {
        super("Room " + roomNumber + " is not available for the requested dates.");
        this.roomNumber = roomNumber;
    }

    /** @return The room number that triggered the exception. */
    public String getRoomNumber() { return roomNumber; }
}
