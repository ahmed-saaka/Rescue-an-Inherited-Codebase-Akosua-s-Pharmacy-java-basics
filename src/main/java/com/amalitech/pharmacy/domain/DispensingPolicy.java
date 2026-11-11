// Contract for validating medicine-category rules without adding category branches to Pharmacy.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public interface DispensingPolicy {
    // Use a policy-provided label for inventory reports and other category displays.
    String category();

    // Empty means the request meets this medicine category's dispensing rules.
    Optional<String> validationError(DispenseRequest request);

    // Most categories do not require a prescriber; prescription-based policies opt in.
    default boolean prescriberRequired() {
        return false;
    }
}
