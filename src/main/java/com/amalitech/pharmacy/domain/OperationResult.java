// Result value returned by a pharmacy operation to report success or a user-readable rejection.
package com.amalitech.pharmacy.domain;

import java.util.Objects;

// Business-rule rejections return false; invalid requests or values that violate invariants throw.
public record OperationResult(boolean successful, String message) {
    public OperationResult {
        Objects.requireNonNull(message, "Result message is required");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Result message cannot be blank");
        }
    }

    public static OperationResult success(String message) {
        return new OperationResult(true, message);
    }

    public static OperationResult failure(String message) {
        return new OperationResult(false, message);
    }
}
