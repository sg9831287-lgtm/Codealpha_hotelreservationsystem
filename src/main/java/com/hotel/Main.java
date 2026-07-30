package com.hotel;

import com.hotel.repository.ReservationRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.BookingService;
import com.hotel.service.PaymentProcessor;
import com.hotel.ui.ConsoleUI;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Application entry point for the Hotel Reservation System.
 *
 * <p>Bootstraps the dependency graph in the correct order (repositories → services → UI),
 * configures JUL logging to display service-layer diagnostics, and delegates
 * control to {@link ConsoleUI#start()}.
 *
 * <p>Any {@link IOException} thrown during repository initialisation (e.g., missing
 * write permissions on the {@code data/} directory) is caught here and reported
 * cleanly before a non-zero exit code is returned to the OS.
 */
public class Main {

    /**
     * @param args command-line arguments; not used by this application
     */
    public static void main(String[] args) {
        configureLogging();

        try {
            RoomRepository        roomRepo        = new RoomRepository();
            ReservationRepository reservationRepo = new ReservationRepository();
            PaymentProcessor      paymentProc     = new PaymentProcessor(reservationRepo);
            BookingService        bookingService  = new BookingService(roomRepo, reservationRepo, paymentProc);
            ConsoleUI             ui              = new ConsoleUI(bookingService);

            ui.start();

        } catch (IOException e) {
            System.err.println("[FATAL] Failed to initialise data repositories: " + e.getMessage());
            System.err.println("Ensure the application has write permissions to the current directory.");
            System.exit(1);
        }
    }

    /**
     * Configures the Java Util Logging framework to emit service-layer INFO messages
     * to the console in a concise single-line format, suppressing verbose JVM noise.
     */
    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(java.util.logging.LogRecord record) {
                return String.format("[%s] %s: %s%n",
                        record.getLevel().getName(),
                        record.getSourceClassName().replaceAll(".*\\.", ""),
                        record.getMessage());
            }
        });

        rootLogger.getHandlers()[0].setLevel(Level.WARNING);
        rootLogger.addHandler(handler);
    }
}
