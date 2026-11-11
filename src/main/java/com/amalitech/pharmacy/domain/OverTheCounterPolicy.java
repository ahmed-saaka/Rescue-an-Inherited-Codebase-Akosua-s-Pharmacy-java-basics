// OTC dispensing policy: accepts requests without prescription-specific validation.
package com.amalitech.pharmacy.domain;

import java.util.Optional;

public final class OverTheCounterPolicy implements DispensingPolicy {
    @Override
    public String category() {
        // Keep the category label in the policy so reports use the same source as validation.
        return "OTC";
    }

    @Override
    public Optional<String> validationError(DispenseRequest request) {
        // OTC medicine has no prescription-specific requirement, so every request passes this policy.
        return Optional.empty();
    }
}
