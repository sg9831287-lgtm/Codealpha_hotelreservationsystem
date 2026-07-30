package com.hotel.domain;

/**
 * Enumerates the distinct room categories available in the hotel,
 * each carrying its own nightly rate and occupancy ceiling.
 */
public enum RoomType {

    STANDARD("Standard", 99.00, 2),
    DELUXE("Deluxe", 179.00, 3),
    SUITE("Suite", 349.00, 4);

    private final String displayName;
    private final double nightlyRate;
    private final int maxOccupancy;

    RoomType(String displayName, double nightlyRate, int maxOccupancy) {
        this.displayName   = displayName;
        this.nightlyRate   = nightlyRate;
        this.maxOccupancy  = maxOccupancy;
    }

    /** @return Human-readable label used in receipts and menus. */
    public String getDisplayName() { return displayName; }

    /** @return Base cost per night in USD before taxes or fees. */
    public double getNightlyRate()  { return nightlyRate; }

    /** @return Maximum number of guests permitted in this room category. */
    public int getMaxOccupancy()    { return maxOccupancy; }

    @Override
    public String toString() { return displayName; }
}
