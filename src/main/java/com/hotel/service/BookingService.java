package com.hotel.service;

import com.hotel.domain.*;
import com.hotel.exception.InvalidBookingIdException;
import com.hotel.exception.PaymentException;
import com.hotel.exception.RoomUnavailableException;
import com.hotel.repository.ReservationRepository;
import com.hotel.repository.RoomRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Orchestrates the complete reservation lifecycle: searching, booking, payment,
 * cancellation, and receipt generation.
 *
 * <p>All operations that mutate room availability acquire the room record,
 * modify it, and immediately flush it back to disk via {@link RoomRepository#save(Object)}.
 * This ensures that the persisted state is consistent with in-memory state even
 * when the JVM terminates unexpectedly between operations.
 */
public class BookingService {

    private static final Logger LOG = Logger.getLogger(BookingService.class.getName());

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentProcessor paymentProcessor;

    /**
     * @param roomRepository        repository used to read and mutate room availability
     * @param reservationRepository repository used to persist and retrieve reservations
     * @param paymentProcessor      service that handles charge and refund transactions
     */
    public BookingService(RoomRepository roomRepository,
                          ReservationRepository reservationRepository,
                          PaymentProcessor paymentProcessor) {
        this.roomRepository        = roomRepository;
        this.reservationRepository = reservationRepository;
        this.paymentProcessor      = paymentProcessor;
    }

    /**
     * Returns all rooms currently marked as available, sorted by floor.
     *
     * @return unmodifiable snapshot of available rooms
     * @throws IOException if the room data file cannot be read
     */
    public List<Room> searchAvailableRooms() throws IOException {
        return roomRepository.findAvailable();
    }

    /**
     * Returns available rooms filtered to the specified category.
     *
     * @param type the room category to filter by
     * @return filtered list of available rooms, never {@code null}
     * @throws IOException if the room data file cannot be read
     */
    public List<Room> searchAvailableRoomsByType(RoomType type) throws IOException {
        return roomRepository.findAvailableByType(type);
    }

    /**
     * Creates a new reservation and processes payment atomically.
     *
     * <p>The sequence is:
     * <ol>
     *   <li>Verify the target room is still available (fail fast with {@link RoomUnavailableException}).</li>
     *   <li>Mark the room as unavailable and persist it immediately.</li>
     *   <li>Create the {@link Reservation} in {@code PENDING} state and persist it.</li>
     *   <li>Attempt payment via {@link PaymentProcessor}.</li>
     *   <li>On payment success, transition the reservation to {@code CONFIRMED} and persist.</li>
     *   <li>On payment failure, roll back room availability and delete the pending reservation.</li>
     * </ol>
     *
     * @param guest     the guest making the booking
     * @param roomNumber the room identifier to reserve
     * @param checkIn   the intended arrival date
     * @param checkOut  the intended departure date; must be strictly after {@code checkIn}
     * @return the confirmed {@link Reservation} with {@code PAID} status
     * @throws RoomUnavailableException if the room is already booked
     * @throws PaymentException         if the payment gateway simulation declines the charge
     * @throws IOException              if any persistence operation fails
     */
    public Reservation createReservation(User guest, String roomNumber,
                                         LocalDate checkIn, LocalDate checkOut)
            throws RoomUnavailableException, PaymentException, IOException {

        Room room = roomRepository.findById(roomNumber)
                .orElseThrow(() -> new RoomUnavailableException(roomNumber));

        if (!room.isAvailable()) {
            throw new RoomUnavailableException(roomNumber);
        }

        room.setAvailable(false);
        roomRepository.save(room);
        LOG.info("Room " + roomNumber + " marked unavailable (pending payment).");

        Reservation reservation = new Reservation(guest, room, checkIn, checkOut);
        reservationRepository.save(reservation);

        try {
            paymentProcessor.charge(reservation);
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
            LOG.info("Reservation confirmed: " + reservation.getBookingId());
            return reservation;

        } catch (PaymentException e) {
            // Payment rollback: release the room and remove the pending record
            room.setAvailable(true);
            roomRepository.save(room);
            reservationRepository.deleteById(reservation.getBookingId());
            LOG.warning("Payment failed for booking " + reservation.getBookingId() +
                        "; room " + roomNumber + " released.");
            throw e;
        }
    }

    /**
     * Cancels an existing reservation and issues a refund if the reservation was paid.
     *
     * <p>Only {@code CONFIRMED} or {@code PENDING} reservations may be cancelled;
     * attempting to cancel an already-cancelled reservation is a no-op guard.
     *
     * @param bookingId the unique booking reference to cancel
     * @throws InvalidBookingIdException if no reservation matches the given ID
     * @throws IOException               if persistence fails during cancellation
     */
    public void cancelReservation(String bookingId)
            throws InvalidBookingIdException, IOException {

        Reservation reservation = reservationRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingIdException(bookingId));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            LOG.warning("Cancellation requested for already-cancelled booking: " + bookingId);
            return;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getPaymentStatus() == PaymentStatus.PAID) {
            paymentProcessor.refund(reservation);
        }

        reservationRepository.save(reservation);

        Room room = roomRepository.findById(reservation.getRoom().getRoomNumber())
                .orElse(reservation.getRoom());
        room.setAvailable(true);
        roomRepository.save(room);

        LOG.info("Reservation cancelled and room released: " + bookingId);
    }

    /**
     * Retrieves a single reservation by its booking ID.
     *
     * @param bookingId the booking reference to look up
     * @return the matching {@link Reservation}
     * @throws InvalidBookingIdException if no reservation is found for the given ID
     * @throws IOException               if the data file cannot be read
     */
    public Reservation findReservation(String bookingId)
            throws InvalidBookingIdException, IOException {

        return reservationRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingIdException(bookingId));
    }

    /**
     * Retrieves all reservations associated with a guest's email address.
     *
     * @param email the guest email to search by, matched case-insensitively
     * @return list of reservations ordered by check-in descending; never {@code null}
     * @throws IOException if the data file cannot be read
     */
    public List<Reservation> findReservationsByGuest(String email) throws IOException {
        return reservationRepository.findByGuestEmail(email);
    }

    /**
     * Returns all non-cancelled reservations currently stored.
     *
     * @return list of active reservations sorted by check-in date
     * @throws IOException if the data file cannot be read
     */
    public List<Reservation> listActiveReservations() throws IOException {
        return reservationRepository.findActive();
    }
}
