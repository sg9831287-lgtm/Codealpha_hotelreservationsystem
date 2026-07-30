package com.hotel.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a physical hotel room with its category, floor location,
 * and current availability. Instances are persisted by {@code RoomRepository}.
 *
 * <p>This class is intentionally kept as a pure data carrier; business rules
 * that govern state transitions live in the service layer.
 */
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String roomNumber;
    private final RoomType type;
    private final int floor;
    private boolean available;

    /**
     * @param roomNumber unique identifier displayed on room keys and receipts (e.g., "201")
     * @param type       the category that determines nightly rate and occupancy limits
     * @param floor      physical floor the room occupies, used for sorting in search results
     */
    public Room(String roomNumber, RoomType type, int floor) {
        this.roomNumber = Objects.requireNonNull(roomNumber, "roomNumber must not be null");
        this.type       = Objects.requireNonNull(type, "type must not be null");
        this.floor      = floor;
        this.available  = true;
    }

    /** @return The unique room identifier (e.g., "101", "301"). */
    public String getRoomNumber() { return roomNumber; }

    /** @return The {@link RoomType} governing pricing and occupancy. */
    public RoomType getType()     { return type; }

    /** @return Floor number on which this room is located. */
    public int getFloor()         { return floor; }

    /**
     * @return {@code true} if the room is not currently assigned to any active reservation.
     */
    public boolean isAvailable()  { return available; }

    /**
     * Marks this room as reserved or released.
     *
     * @param available {@code false} when a booking is confirmed; {@code true} after cancellation.
     */
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room other = (Room) o;
        return roomNumber.equals(other.roomNumber);
    }

    @Override
    public int hashCode() { return roomNumber.hashCode(); }

    @Override
    public String toString() {
        return String.format("Room %-4s | %-8s | Floor %d | Rate: $%.2f/night | %s",
                roomNumber, type.getDisplayName(), floor,
                type.getNightlyRate(), available ? "AVAILABLE" : "BOOKED");
    }
}
