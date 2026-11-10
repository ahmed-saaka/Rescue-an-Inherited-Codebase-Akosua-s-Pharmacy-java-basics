// Prescription policy: rejects dispensing requests that lack a named prescriber.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public final class PrescriptionPolicy implements DispensingPolicy {
    @Override
    public String category() {
        return "PRESCRIPTION";
    }

    @Override
    public boolean prescriberRequired() {
        return true;
    }

    @Override
    public Optional<String> validationError(DispenseRequest request) {
        // Prescription medicine cannot be dispensed without a named prescriber.
        if (request.prescriber() == null || request.prescriber().isBlank()) {
            return Optional.of("without a prescriber");
        }
        return Optional.empty();
    }
}
