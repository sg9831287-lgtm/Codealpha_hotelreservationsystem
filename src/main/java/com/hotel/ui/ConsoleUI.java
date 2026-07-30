package com.hotel.ui;

import com.hotel.domain.*;
import com.hotel.exception.InvalidBookingIdException;
import com.hotel.exception.PaymentException;
import com.hotel.exception.RoomUnavailableException;
import com.hotel.service.BookingService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Console-based user interface that drives all guest interactions for the
 * Hotel Reservation System.
 *
 * <p>This class is purely responsible for input gathering, output rendering, and
 * routing user selections to the appropriate {@link BookingService} operations.
 * No business logic should reside here.
 */
public class ConsoleUI {

    private static final String BORDER = "=".repeat(65);
    private static final String THIN   = "-".repeat(65);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BookingService bookingService;
    private final Scanner scanner;

    /**
     * @param bookingService the service layer delegate handling all booking logic
     */
    public ConsoleUI(BookingService bookingService) {
        this.bookingService = bookingService;
        this.scanner        = new Scanner(System.in);
    }

    /**
     * Enters the main application loop. The loop continues until the user selects
     * the exit option (0), at which point the scanner is closed and the method returns.
     */
    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntSafely();
            switch (choice) {
                case 1 -> handleSearchAllAvailable();
                case 2 -> handleSearchByType();
                case 3 -> handleNewBooking();
                case 4 -> handleCancelBooking();
                case 5 -> handleViewReceipt();
                case 6 -> handleMyReservations();
                case 7 -> handleAllActiveReservations();
                case 0 -> { running = false; printLine("Thank you for choosing Grand Horizon Hotel. Goodbye!"); }
                default -> printError("Invalid selection. Please enter a number from the menu.");
            }
        }
        scanner.close();
    }

    // -------------------------------------------------------------------------
    // Menu handlers
    // -------------------------------------------------------------------------

    private void handleSearchAllAvailable() {
        printSectionHeader("Available Rooms");
        try {
            List<Room> rooms = bookingService.searchAvailableRooms();
            if (rooms.isEmpty()) {
                printLine("No rooms are currently available.");
            } else {
                rooms.forEach(r -> printLine("  " + r));
            }
        } catch (IOException e) {
            printError("Unable to retrieve room data: " + e.getMessage());
        }
    }

    private void handleSearchByType() {
        printSectionHeader("Search by Room Category");
        printLine("  1. Standard  ($99.00/night  | Max 2 guests)");
        printLine("  2. Deluxe    ($179.00/night | Max 3 guests)");
        printLine("  3. Suite     ($349.00/night | Max 4 guests)");
        print("  Select category: ");

        int choice = readIntSafely();
        RoomType type = switch (choice) {
            case 1 -> RoomType.STANDARD;
            case 2 -> RoomType.DELUXE;
            case 3 -> RoomType.SUITE;
            default -> null;
        };
        if (type == null) {
            printError("Invalid category selection.");
            return;
        }

        try {
            List<Room> rooms = bookingService.searchAvailableRoomsByType(type);
            printSectionHeader("Available " + type.getDisplayName() + " Rooms");
            if (rooms.isEmpty()) {
                printLine("No " + type.getDisplayName() + " rooms are currently available.");
            } else {
                rooms.forEach(r -> printLine("  " + r));
            }
        } catch (IOException e) {
            printError("Unable to retrieve room data: " + e.getMessage());
        }
    }

    private void handleNewBooking() {
        printSectionHeader("New Reservation");
        try {
            String name  = readNonBlank("  Guest full name: ");
            String email = readNonBlank("  Email address: ");
            String phone = readNonBlank("  Phone number: ");

            User guest;
            try {
                guest = new User(name, email, phone);
            } catch (IllegalArgumentException e) {
                printError(e.getMessage());
                return;
            }

            List<Room> available = bookingService.searchAvailableRooms();
            if (available.isEmpty()) {
                printLine("Sorry, there are no available rooms at this time.");
                return;
            }

            printLine("\n  Available Rooms:");
            available.forEach(r -> printLine("    " + r));

            String roomNumber = readNonBlank("\n  Enter room number to book: ").toUpperCase();
            LocalDate checkIn  = readDate("  Check-in date  (yyyy-MM-dd): ");
            LocalDate checkOut = readDate("  Check-out date (yyyy-MM-dd): ");

            if (checkIn == null || checkOut == null) return;
            if (!checkOut.isAfter(checkIn)) {
                printError("Check-out date must be after check-in date.");
                return;
            }
            if (checkIn.isBefore(LocalDate.now())) {
                printError("Check-in date cannot be in the past.");
                return;
            }

            printLine("\n  Processing payment, please wait...");
            Reservation confirmed = bookingService.createReservation(guest, roomNumber, checkIn, checkOut);
            printBookingReceipt(confirmed);

        } catch (RoomUnavailableException e) {
            printError("Room " + e.getRoomNumber() + " is not available. Please choose another room.");
        } catch (PaymentException e) {
            printError("Payment failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            printError("Invalid dates: " + e.getMessage());
        } catch (IOException e) {
            printError("A system error occurred while saving your reservation: " + e.getMessage());
        }
    }

    private void handleCancelBooking() {
        printSectionHeader("Cancel Reservation");
        String bookingId = readNonBlank("  Enter Booking ID to cancel: ").toUpperCase();
        print("  Confirm cancellation of " + bookingId + "? (yes/no): ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes")) {
            printLine("  Cancellation aborted.");
            return;
        }

        try {
            bookingService.cancelReservation(bookingId);
            printLine("\n  ✔ Reservation " + bookingId + " has been cancelled. A full refund will be issued.");
        } catch (InvalidBookingIdException e) {
            printError("Booking not found: " + e.getBookingId());
        } catch (IOException e) {
            printError("A system error occurred during cancellation: " + e.getMessage());
        }
    }

    private void handleViewReceipt() {
        printSectionHeader("View Booking Receipt");
        String bookingId = readNonBlank("  Enter Booking ID: ").toUpperCase();
        try {
            Reservation reservation = bookingService.findReservation(bookingId);
            printBookingReceipt(reservation);
        } catch (InvalidBookingIdException e) {
            printError("No booking found with ID: " + e.getBookingId());
        } catch (IOException e) {
            printError("Unable to retrieve reservation data: " + e.getMessage());
        }
    }

    private void handleMyReservations() {
        printSectionHeader("My Reservations");
        String email = readNonBlank("  Enter your email address: ");
        try {
            List<Reservation> reservations = bookingService.findReservationsByGuest(email);
            if (reservations.isEmpty()) {
                printLine("  No reservations found for: " + email);
            } else {
                reservations.forEach(r -> printLine("  " + r));
            }
        } catch (IOException e) {
            printError("Unable to retrieve reservations: " + e.getMessage());
        }
    }

    private void handleAllActiveReservations() {
        printSectionHeader("All Active Reservations");
        try {
            List<Reservation> reservations = bookingService.listActiveReservations();
            if (reservations.isEmpty()) {
                printLine("  No active reservations in the system.");
            } else {
                reservations.forEach(r -> printLine("  " + r));
            }
        } catch (IOException e) {
            printError("Unable to retrieve reservations: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Receipt renderer
    // -------------------------------------------------------------------------

    /**
     * Renders a formatted, human-readable booking receipt to standard output.
     *
     * @param r the reservation whose details are to be printed
     */
    public void printBookingReceipt(Reservation r) {
        System.out.println("\n" + BORDER);
        System.out.println("         GRAND HORIZON HOTEL — BOOKING RECEIPT");
        System.out.println(BORDER);
        System.out.printf("  Booking ID    : %s%n",   r.getBookingId());
        System.out.printf("  Status        : %s%n",   r.getStatus());
        System.out.println(THIN);
        System.out.println("  GUEST DETAILS");
        System.out.printf("  Name          : %s%n",   r.getGuest().getName());
        System.out.printf("  Email         : %s%n",   r.getGuest().getEmail());
        System.out.printf("  Phone         : %s%n",   r.getGuest().getPhone());
        System.out.println(THIN);
        System.out.println("  ROOM DETAILS");
        System.out.printf("  Room Number   : %s%n",   r.getRoom().getRoomNumber());
        System.out.printf("  Category      : %s%n",   r.getRoom().getType().getDisplayName());
        System.out.printf("  Floor         : %d%n",   r.getRoom().getFloor());
        System.out.printf("  Max Occupancy : %d guests%n", r.getRoom().getType().getMaxOccupancy());
        System.out.println(THIN);
        System.out.println("  STAY DETAILS");
        System.out.printf("  Check-In      : %s%n",   r.getCheckIn().format(DATE_FMT));
        System.out.printf("  Check-Out     : %s%n",   r.getCheckOut().format(DATE_FMT));
        System.out.printf("  Duration      : %d night(s)%n", r.getNights());
        System.out.println(THIN);
        System.out.println("  PAYMENT SUMMARY");
        double base = r.getNights() * r.getRoom().getType().getNightlyRate();
        double tax  = r.getTotalAmount() - base;
        System.out.printf("  Rate          : $%.2f/night%n", r.getRoom().getType().getNightlyRate());
        System.out.printf("  Subtotal      : $%.2f%n",  base);
        System.out.printf("  Tax (10%%)     : $%.2f%n",  tax);
        System.out.printf("  Total Charged : $%.2f%n",  r.getTotalAmount());
        System.out.printf("  Payment       : %s%n",    r.getPaymentStatus());
        System.out.println(BORDER + "\n");
    }

    // -------------------------------------------------------------------------
    // Input helpers
    // -------------------------------------------------------------------------

    private int readIntSafely() {
        while (true) {
            try {
                int value = scanner.nextInt();
                scanner.nextLine();
                return value;
            } catch (InputMismatchException e) {
                scanner.nextLine();
                printError("Please enter a valid integer.");
                print("  Selection: ");
            }
        }
    }

    private String readNonBlank(String prompt) {
        String input;
        do {
            print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                printError("This field cannot be blank.");
            }
        } while (input.isEmpty());
        return input;
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                return LocalDate.parse(raw, DATE_FMT);
            } catch (DateTimeParseException e) {
                printError("Invalid date format. Please use yyyy-MM-dd (e.g., 2025-12-25).");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Print utilities
    // -------------------------------------------------------------------------

    private void printBanner() {
        System.out.println("\n" + BORDER);
        System.out.println("        GRAND HORIZON HOTEL RESERVATION SYSTEM");
        System.out.println("                  Powered by HotelOS v1.0");
        System.out.println(BORDER);
    }

    private void printMainMenu() {
        System.out.println("\n" + THIN);
        System.out.println("  MAIN MENU");
        System.out.println(THIN);
        System.out.println("  1. View All Available Rooms");
        System.out.println("  2. Search Rooms by Category");
        System.out.println("  3. Make a New Reservation");
        System.out.println("  4. Cancel a Reservation");
        System.out.println("  5. View Booking Receipt");
        System.out.println("  6. My Reservations (by Email)");
        System.out.println("  7. All Active Reservations");
        System.out.println("  0. Exit");
        System.out.println(THIN);
        print("  Selection: ");
    }

    private void printSectionHeader(String title) {
        System.out.println("\n" + BORDER);
        System.out.println("  " + title.toUpperCase());
        System.out.println(BORDER);
    }

    private void printLine(String message) {
        System.out.println(message);
    }

    private void print(String message) {
        System.out.print(message);
    }

    private void printError(String message) {
        System.out.println("\n  [ERROR] " + message + "\n");
    }
}
