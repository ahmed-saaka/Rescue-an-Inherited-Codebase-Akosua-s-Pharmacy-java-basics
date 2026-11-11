// Immutable input to a dispense operation.
package com.amalitech.pharmacy.domain;

import java.util.Objects;

public record DispenseRequest(int quantity, String prescriber, CategoryDetails categoryDetails) {
    public DispenseRequest {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Dispense quantity must be positive");
        }
        prescriber = prescriber == null ? null : prescriber.strip();
        Objects.requireNonNull(categoryDetails, "Category details are required");
    }

    public DispenseRequest(int quantity, String prescriber) {
        this(quantity, prescriber, new NoCategoryDetails());
    }

    public sealed interface CategoryDetails permits NoCategoryDetails, ControlledSubstanceDetails {
    }

    public record NoCategoryDetails() implements CategoryDetails {
    }

    public record ControlledSubstanceDetails(String permitNumber) implements CategoryDetails {
        public ControlledSubstanceDetails {
            if (permitNumber == null || permitNumber.isBlank()) {
                throw new IllegalArgumentException("Permit number cannot be blank");
            }
        }
    }
}
