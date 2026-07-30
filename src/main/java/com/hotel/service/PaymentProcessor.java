package com.hotel.service;

import com.hotel.domain.PaymentStatus;
import com.hotel.domain.Reservation;
import com.hotel.exception.PaymentException;
import com.hotel.repository.ReservationRepository;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Simulates payment processing for hotel reservations.
 *
 * <p>In a production integration this class would delegate to a payment gateway
 * (e.g., Stripe, Braintree) over HTTPS. Here, a configurable failure rate
 * mirrors real-world declined-card scenarios so that the error-handling paths
 * can be exercised during manual testing.
 *
 * <p>On success, the reservation's {@link PaymentStatus} is updated to
 * {@code PAID} and immediately persisted. On refund, it is set to {@code REFUNDED}.
 */
public class PaymentProcessor {

    private static final Logger LOG = Logger.getLogger(PaymentProcessor.class.getName());
    private static final double SIMULATED_FAILURE_RATE = 0.10;

    private final ReservationRepository reservationRepository;
    private final Random random;

    /**
     * @param reservationRepository repository used to persist payment status updates
     */
    public PaymentProcessor(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
        this.random                = new Random();
    }

    /**
     * Attempts to charge the full amount due on the given reservation.
     *
     * <p>A {@link PaymentException} is thrown with a 10 % probability to simulate
     * gateway failures. The caller is responsible for deciding whether to retry
     * or abort the booking workflow.
     *
     * @param reservation the reservation whose {@code totalAmount} is to be charged
     * @throws PaymentException if the simulated gateway declines the transaction
     * @throws IOException      if the updated reservation cannot be persisted
     */
    public void charge(Reservation reservation) throws PaymentException, IOException {
        LOG.info(String.format("Initiating charge of $%.2f for booking %s",
                reservation.getTotalAmount(), reservation.getBookingId()));

        if (random.nextDouble() < SIMULATED_FAILURE_RATE) {
            throw new PaymentException(
                    "Transaction declined by payment gateway. Please verify card details and retry.");
        }

        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservationRepository.save(reservation);

        LOG.info("Payment confirmed for booking " + reservation.getBookingId());
    }

    /**
     * Issues a full refund for a reservation that is being cancelled.
     *
     * <p>Refunds are always granted in this simulation; a real implementation
     * would apply cancellation policy rules before issuing credit.
     *
     * @param reservation the reservation to refund; must have {@code PAID} status
     * @throws IOException if the updated reservation cannot be persisted
     */
    public void refund(Reservation reservation) throws IOException {
        LOG.info(String.format("Processing refund of $%.2f for booking %s",
                reservation.getTotalAmount(), reservation.getBookingId()));

        reservation.setPaymentStatus(PaymentStatus.REFUNDED);
        reservationRepository.save(reservation);

        LOG.info("Refund issued for booking " + reservation.getBookingId());
    }
}
