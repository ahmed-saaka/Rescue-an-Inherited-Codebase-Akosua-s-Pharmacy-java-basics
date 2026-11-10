package com.amalitech.pharmacy.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Customer {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final String name;
    private final CustomerType type;
    private final String phone;
    private final List<String> history = new ArrayList<>();

    public Customer(String name, CustomerType type) {
        this(name, type, null);
    }

    public Customer(String name, CustomerType type, String phone) {
        this.name = requireText(name, "Customer name");
        this.type = Objects.requireNonNull(type, "Customer type is required");
        this.id = NEXT_ID.getAndIncrement();
        this.phone = phone;
    }

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

    public synchronized List<String> history() {
        return List.copyOf(history);
    }

    synchronized void recordDispense(String entry) {
        history.add(requireText(entry, "History entry"));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
