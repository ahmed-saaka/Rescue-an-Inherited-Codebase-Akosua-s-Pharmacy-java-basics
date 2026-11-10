package com.amalitech.pharmacy.domain;

import java.util.Map;
import java.math.BigDecimal;
import java.util.Objects;

public record Transaction(
        int id,
        String customerName,
        String medicationName,
        int quantity,
        BigDecimal total,
        TransactionType type,
        String prescriber,
        Map<String, String> dispensingDetails) {

    public Transaction {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction id must be positive");
        }
        customerName = requireText(customerName, "Customer name");
        medicationName = requireText(medicationName, "Medication name");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be positive");
        }
        Objects.requireNonNull(total, "Transaction total is required");
        if (total.signum() < 0) {
            throw new IllegalArgumentException("Transaction total must be non-negative");
        }
        Objects.requireNonNull(type, "Transaction type is required");
        Objects.requireNonNull(dispensingDetails, "Dispensing details are required");
        dispensingDetails = Map.copyOf(dispensingDetails);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
