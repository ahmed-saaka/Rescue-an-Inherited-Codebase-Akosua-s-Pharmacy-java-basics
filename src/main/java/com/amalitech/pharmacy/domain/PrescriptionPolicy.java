// Prescription policy: rejects dispensing requests that lack a named prescriber.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public final class PrescriptionPolicy implements DispensingPolicy {
    @Override
    public String category() {
        // Keep the displayed category consistent with this policy's prescription rules.
        return "PRESCRIPTION";
    }

    @Override
    public boolean prescriberRequired() {
        return true;
    }

    @Override
    public Optional<String> validationError(DispenseRequest request) {
        // Return a user-readable reason instead of throwing for an ordinary rejected request.
        if (request.prescriber() == null || request.prescriber().isBlank()) {
            return Optional.of("without a prescriber");
        }
        return Optional.empty();
    }
}
