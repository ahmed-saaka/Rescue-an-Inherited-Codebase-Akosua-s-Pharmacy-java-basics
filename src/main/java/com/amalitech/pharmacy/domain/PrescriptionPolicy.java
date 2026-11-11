// Prescription policy: rejects dispensing requests that lack a named prescriber.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public final class PrescriptionPolicy implements DispensingPolicy {
    @Override
    public String category() {
        return "PRESCRIPTION";
    }

    @Override
    public Optional<String> validationError(DispenseRequest request) {
        // Return a user-readable reason instead of throwing for an ordinary rejected request.
        if (request.prescriber() == null || request.prescriber().isBlank()) {
            return Optional.of("A prescriber is required for prescription medicine.");
        }
        return Optional.empty();
    }
}
