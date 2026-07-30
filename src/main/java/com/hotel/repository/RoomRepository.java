package com.hotel.repository;

import com.hotel.domain.Room;
import com.hotel.domain.RoomType;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * File-backed repository that persists {@link Room} state using Java serialization.
 *
 * <p>A {@link ReentrantReadWriteLock} guards all I/O operations so that concurrent
 * reads never observe a partially-written file and writes are mutually exclusive.
 * On first launch, when no data file is present, the repository seeds itself with
 * a default room inventory so the application is immediately usable.
 */
public class RoomRepository implements FileRepository<Room> {

    private static final String DATA_FILE = "data/rooms.dat";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs the repository and ensures the backing data directory exists.
     * If no prior room file is found, a default inventory is generated and persisted.
     *
     * @throws IOException if the data directory cannot be created or the seed data
     *                     cannot be written on first launch
     */
    public RoomRepository() throws IOException {
        Files.createDirectories(Paths.get("data"));
        if (!Files.exists(Paths.get(DATA_FILE))) {
            seedDefaultInventory();
        }
    }

    @Override
    public void save(Room room) throws IOException {
        lock.writeLock().lock();
        try {
            List<Room> all = loadFromDisk();
            all.removeIf(r -> r.getRoomNumber().equals(room.getRoomNumber()));
            all.add(room);
            flushToDisk(all);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Room> findById(String roomNumber) throws IOException {
        lock.readLock().lock();
        try {
            return loadFromDisk().stream()
                    .filter(r -> r.getRoomNumber().equals(roomNumber))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Room> findAll() throws IOException {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(loadFromDisk());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteById(String roomNumber) throws IOException {
        lock.writeLock().lock();
        try {
            List<Room> all = loadFromDisk();
            all.removeIf(r -> r.getRoomNumber().equals(roomNumber));
            flushToDisk(all);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns all rooms whose availability flag is set to {@code true}.
     *
     * @return list of currently available rooms, sorted by floor then room number
     * @throws IOException if the data file cannot be read
     */
    public List<Room> findAvailable() throws IOException {
        return findAll().stream()
                .filter(Room::isAvailable)
                .sorted(Comparator.comparingInt(Room::getFloor)
                        .thenComparing(Room::getRoomNumber))
                .collect(Collectors.toList());
    }

    /**
     * Returns available rooms filtered to a specific {@link RoomType}.
     *
     * @param type the category to filter by
     * @return filtered and sorted list of available rooms of the given type
     * @throws IOException if the data file cannot be read
     */
    public List<Room> findAvailableByType(RoomType type) throws IOException {
        return findAvailable().stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Room> loadFromDisk() throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(DATA_FILE)))) {
            return (List<Room>) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Corrupted room data file; class version mismatch.", e);
        }
    }

    private void flushToDisk(List<Room> rooms) throws IOException {
        Path target = Paths.get(DATA_FILE);
        Path temp   = Paths.get(DATA_FILE + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(temp.toFile())))) {
            oos.writeObject(rooms);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void seedDefaultInventory() throws IOException {
        List<Room> seed = new ArrayList<>();

        // Floor 1 — Standard rooms 101–105
        for (int i = 1; i <= 5; i++) {
            seed.add(new Room("10" + i, RoomType.STANDARD, 1));
        }
        // Floor 2 — Deluxe rooms 201–203
        for (int i = 1; i <= 3; i++) {
            seed.add(new Room("20" + i, RoomType.DELUXE, 2));
        }
        // Floor 3 — Suites 301–302
        seed.add(new Room("301", RoomType.SUITE, 3));
        seed.add(new Room("302", RoomType.SUITE, 3));

        flushToDisk(seed);
    }
}
