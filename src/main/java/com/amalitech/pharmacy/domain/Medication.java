// Medication model: owns price, category policy, and protected stock-changing operations.
package com.amalitech.pharmacy.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Medication {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final String name;
    private final BigDecimal price;
    private final DispensingPolicy dispensingPolicy;
    private int stock;

    public Medication(String name, BigDecimal price, int initialStock, DispensingPolicy dispensingPolicy) {
        this.name = requireText(name, "Medication name");
        this.price = Objects.requireNonNull(price, "Price is required");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Price must be a non-negative amount");
        }
        // Reject invalid starting inventory as well as invalid later stock changes.
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        this.dispensingPolicy = Objects.requireNonNull(dispensingPolicy, "Dispensing policy is required");
        this.id = NEXT_ID.getAndIncrement();
        this.stock = initialStock;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public BigDecimal price() {
        return price;
    }

    public String category() {
        return dispensingPolicy.category();
    }

    public DispensingPolicy dispensingPolicy() {
        return dispensingPolicy;
    }

    public synchronized int stock() {
        return stock;
    }

    // Keep stock changes inside the domain package so callers cannot bypass Pharmacy's checks and audit.
    synchronized boolean dispenseStock(int quantity) {
        // Refuse invalid quantities and shortages before subtracting anything.
        if (quantity <= 0 || quantity > stock) {
            return false;
        }
        stock -= quantity;
        return true;
    }

    synchronized boolean restock(int quantity) {
        // Refuse invalid quantities and integer overflow before adding to stock.
        if (quantity <= 0 || quantity > Integer.MAX_VALUE - stock) {
            return false;
        }
        stock += quantity;
        return true;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
