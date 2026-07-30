package com.hotel.repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Generic contract for a file-backed repository capable of persisting and
 * retrieving domain entities by their string-valued key.
 *
 * <p>Implementations must guarantee that writes are immediately flushed to
 * disk to maintain consistency in the event of an unexpected JVM termination.
 *
 * @param <T> the domain entity type managed by this repository
 */
public interface FileRepository<T> {

    /**
     * Persists or replaces a single entity. If an entity with the same key
     * already exists it is overwritten; otherwise a new record is created.
     *
     * @param entity the entity to persist
     * @throws IOException if the underlying file cannot be written
     */
    void save(T entity) throws IOException;

    /**
     * Retrieves an entity by its unique key.
     *
     * @param id the string key used to locate the entity
     * @return an {@link Optional} containing the entity, or empty if not found
     * @throws IOException if the data file cannot be read
     */
    Optional<T> findById(String id) throws IOException;

    /**
     * Retrieves all persisted entities managed by this repository.
     *
     * @return an unmodifiable snapshot of all entities; never {@code null}
     * @throws IOException if the data file cannot be read
     */
    List<T> findAll() throws IOException;

    /**
     * Removes the entity identified by the given key.
     *
     * @param id the unique identifier of the entity to remove
     * @throws IOException if the data file cannot be updated
     */
    void deleteById(String id) throws IOException;
}
