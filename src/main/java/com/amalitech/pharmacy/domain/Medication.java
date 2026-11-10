package com.amalitech.pharmacy.domain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Medication {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final String name;
    private final double price;
    private final DispensingPolicy dispensingPolicy;
    private int stock;

    public Medication(String name, double price, int initialStock, DispensingPolicy dispensingPolicy) {
        this.id = NEXT_ID.getAndIncrement();
        this.name = requireText(name, "Medication name");
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Price must be a finite, non-negative amount");
        }
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        this.price = price;
        this.stock = initialStock;
        this.dispensingPolicy = Objects.requireNonNull(dispensingPolicy, "Dispensing policy is required");
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public double price() {
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

    public synchronized boolean dispenseStock(int quantity) {
        if (quantity <= 0 || quantity > stock) {
            return false;
        }
        stock -= quantity;
        return true;
    }

    public synchronized boolean restock(int quantity) {
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
