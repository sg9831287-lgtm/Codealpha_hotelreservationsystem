package com.hotel.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a registered guest who initiates reservations.
 *
 * <p>A {@code User} is identified by their email address, which is validated
 * at construction time to ensure persistence consistency.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    private final String name;
    private final String email;
    private final String phone;

    /**
     * @param name  guest's full name; must not be blank
     * @param email guest's email address; validated against RFC-like pattern
     * @param phone contact number for reservation confirmation
     * @throws IllegalArgumentException if email format is invalid
     */
    public User(String name, String email, String phone) {
        this.name  = Objects.requireNonNull(name,  "name must not be null").trim();
        this.phone = Objects.requireNonNull(phone, "phone must not be null").trim();

        String trimmedEmail = Objects.requireNonNull(email, "email must not be null").trim();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + trimmedEmail);
        }
        this.email = trimmedEmail;
    }

    /** @return Guest's full name as provided at registration. */
    public String getName()  { return name; }

    /** @return Validated email address used as a unique identifier. */
    public String getEmail() { return email; }

    /** @return Guest's contact phone number. */
    public String getPhone() { return phone; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return email.equalsIgnoreCase(((User) o).email);
    }

    @Override
    public int hashCode() { return email.toLowerCase().hashCode(); }

    @Override
    public String toString() {
        return String.format("%s <%s> | Phone: %s", name, email, phone);
    }
}
