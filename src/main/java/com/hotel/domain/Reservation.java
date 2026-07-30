package com.hotel.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Encapsulates a single hotel reservation, tying a {@link User}, a {@link Room},
 * and a date range together with financial and lifecycle state.
 *
 * <p>The total amount is computed deterministically from the room's nightly rate
 * and the length of stay, with a fixed 10 % tax applied on top of the base rate.
 */
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final double TAX_RATE = 0.10;

    private final String bookingId;
    private final User guest;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final double totalAmount;
    private ReservationStatus status;
    private PaymentStatus paymentStatus;

    /**
     * Creates a new reservation and computes the total payable amount.
     *
     * @param guest    the guest making the reservation
     * @param room     the room being reserved
     * @param checkIn  the intended arrival date (inclusive)
     * @param checkOut the intended departure date (exclusive); must be after {@code checkIn}
     * @throws IllegalArgumentException if {@code checkOut} is not strictly after {@code checkIn}
     */
    public Reservation(User guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
        this.bookingId     = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.guest         = Objects.requireNonNull(guest, "guest must not be null");
        this.room          = Objects.requireNonNull(room,  "room must not be null");
        this.checkIn       = checkIn;
        this.checkOut      = checkOut;
        this.status        = ReservationStatus.PENDING;
        this.paymentStatus = PaymentStatus.UNPAID;

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double base = nights * room.getType().getNightlyRate();
        this.totalAmount = base + (base * TAX_RATE);
    }

    /** @return System-generated booking reference (e.g., "BK-A1B2C3D4"). */
    public String getBookingId()         { return bookingId; }

    /** @return The {@link User} who owns this reservation. */
    public User getGuest()               { return guest; }

    /** @return The {@link Room} assigned to this reservation. */
    public Room getRoom()                { return room; }

    /** @return Arrival date (inclusive). */
    public LocalDate getCheckIn()        { return checkIn; }

    /** @return Departure date (exclusive of the final night). */
    public LocalDate getCheckOut()       { return checkOut; }

    /**
     * @return Total charge including base rate and taxes, rounded to the nearest cent
     *         by display formatting rather than stored rounding.
     */
    public double getTotalAmount()       { return totalAmount; }

    /** @return Current lifecycle state of this reservation. */
    public ReservationStatus getStatus() { return status; }

    /** @return Current payment settlement state. */
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    /**
     * Advances the reservation's lifecycle state.
     *
     * @param status the new {@link ReservationStatus} to apply
     */
    public void setStatus(ReservationStatus status) { this.status = status; }

    /**
     * Updates the payment settlement state after a transaction attempt.
     *
     * @param paymentStatus the outcome of the payment operation
     */
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    /**
     * Computes the number of nights covered by this reservation.
     *
     * @return nights between check-in and check-out dates
     */
    public long getNights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        return bookingId.equals(((Reservation) o).bookingId);
    }

    @Override
    public int hashCode() { return bookingId.hashCode(); }

    @Override
    public String toString() {
        return String.format("Booking ID: %s | Room: %s | Guest: %s | %s → %s | $%.2f | %s | %s",
                bookingId, room.getRoomNumber(), guest.getName(),
                checkIn, checkOut, totalAmount, status, paymentStatus);
    }
}
