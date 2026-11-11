// Contract for validating medicine-category rules without adding category branches to Pharmacy.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public interface DispensingPolicy {
    // Use a policy-provided label for inventory reports and other category displays.
    String category();

    // Empty means valid; a present value is a complete user-facing rejection sentence.
    Optional<String> validationError(DispenseRequest request);
}
