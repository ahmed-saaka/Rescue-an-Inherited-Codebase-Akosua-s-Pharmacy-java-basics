// Represents a pharmacy customer and keeps their basic details and dispense history together.
package com.amalitech.pharmacy.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Customer {
    // Generate distinct IDs safely when customers are created from multiple threads.
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    // These details are fixed after construction so a customer's identity stays consistent.
    private final int id;
    private final String name;
    private final CustomerType type;
    private final String phone;

    // Keep an editable list internally; callers receive only an immutable snapshot.
    private final List<String> history = new ArrayList<>();

    // Allow customers to be created without providing a phone number.
    public Customer(String name, CustomerType type) {
        this(name, type, null);
    }

    // Validate required details and assign a unique ID when the customer is created.
    public Customer(String name, CustomerType type, String phone) {
        this.name = requireText(name, "Customer name");
        this.type = Objects.requireNonNull(type, "Customer type is required");
        this.id = NEXT_ID.getAndIncrement();
        this.phone = phone;
    }

    // Record-style accessors expose fields without allowing them to be changed.
    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public CustomerType type() {
        return type;
    }

    public String phone() {
        return phone;
    }

    // Synchronize reads with history updates and prevent callers from modifying the stored list.
    public synchronized List<String> history() {
        return List.copyOf(history);
    }

    // Package-private so pharmacy domain code can record dispensing without exposing this as public API.
    synchronized void recordDispense(String entry) {
        history.add(requireText(entry, "History entry"));
    }

    // Share consistent nonblank-text validation for names and history entries.
    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
