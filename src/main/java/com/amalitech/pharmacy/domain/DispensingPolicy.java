// Contract for validating medicine-category rules without adding category branches to Pharmacy.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public interface DispensingPolicy {
    String category();

    // Empty means the request meets this medicine category's dispensing rules.
    Optional<String> validationError(DispenseRequest request);

    default boolean prescriberRequired() {
        return false;
    }
}
