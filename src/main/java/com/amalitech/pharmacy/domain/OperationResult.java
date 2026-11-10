package com.amalitech.pharmacy.domain;

import java.util.Objects;

public record OperationResult(boolean successful, String message) {
    public OperationResult {
        Objects.requireNonNull(message, "Result message is required");
    }
}
