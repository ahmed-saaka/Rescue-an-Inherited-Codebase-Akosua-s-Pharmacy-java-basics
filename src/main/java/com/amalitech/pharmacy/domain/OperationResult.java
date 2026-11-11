// Result value returned by a pharmacy operation to report success or a user-readable rejection.
package com.amalitech.pharmacy.domain;

import java.util.Objects;

public record OperationResult(boolean successful, String message) {
    public OperationResult {
        // A message is always available for the console or another caller to explain the outcome.
        Objects.requireNonNull(message, "Result message is required");
    }
}
