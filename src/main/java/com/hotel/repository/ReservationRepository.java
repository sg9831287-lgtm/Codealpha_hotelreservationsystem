package com.hotel.repository;

import com.hotel.domain.Reservation;
import com.hotel.domain.ReservationStatus;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * File-backed repository that persists {@link Reservation} records using Java serialization.
 *
 * <p>All disk interactions are protected by a {@link ReentrantReadWriteLock}: multiple
 * threads may read concurrently, but write operations acquire an exclusive lock and
 * perform an atomic replace (write-to-temp, then move) to prevent corruption.
 */
public class ReservationRepository implements FileRepository<Reservation> {

    private static final String DATA_FILE = "data/reservations.dat";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs the repository and ensures the backing data directory and an empty
     * reservation file exist before the first write operation is attempted.
     *
     * @throws IOException if the directory or bootstrap file cannot be created
     */
    public ReservationRepository() throws IOException {
        Files.createDirectories(Paths.get("data"));
        if (!Files.exists(Paths.get(DATA_FILE))) {
            flushToDisk(new ArrayList<>());
        }
    }

    @Override
    public void save(Reservation reservation) throws IOException {
        lock.writeLock().lock();
        try {
            List<Reservation> all = loadFromDisk();
            all.removeIf(r -> r.getBookingId().equals(reservation.getBookingId()));
            all.add(reservation);
            flushToDisk(all);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Reservation> findById(String bookingId) throws IOException {
        lock.readLock().lock();
        try {
            return loadFromDisk().stream()
                    .filter(r -> r.getBookingId().equalsIgnoreCase(bookingId))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Reservation> findAll() throws IOException {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(loadFromDisk());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteById(String bookingId) throws IOException {
        lock.writeLock().lock();
        try {
            List<Reservation> all = loadFromDisk();
            all.removeIf(r -> r.getBookingId().equalsIgnoreCase(bookingId));
            flushToDisk(all);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns only active (non-cancelled) reservations, sorted by check-in date ascending.
     *
     * @return list of active reservations; never {@code null}
     * @throws IOException if the data file cannot be read
     */
    public List<Reservation> findActive() throws IOException {
        return findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .sorted(Comparator.comparing(Reservation::getCheckIn))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all reservations belonging to a guest identified by email address.
     *
     * @param email the guest's email, matched case-insensitively
     * @return list of reservations for the given guest; never {@code null}
     * @throws IOException if the data file cannot be read
     */
    public List<Reservation> findByGuestEmail(String email) throws IOException {
        return findAll().stream()
                .filter(r -> r.getGuest().getEmail().equalsIgnoreCase(email))
                .sorted(Comparator.comparing(Reservation::getCheckIn).reversed())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Reservation> loadFromDisk() throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(DATA_FILE)))) {
            return (List<Reservation>) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Corrupted reservation data file; class version mismatch.", e);
        }
    }

    private void flushToDisk(List<Reservation> reservations) throws IOException {
        Path target = Paths.get(DATA_FILE);
        Path temp   = Paths.get(DATA_FILE + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(temp.toFile())))) {
            oos.writeObject(reservations);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
