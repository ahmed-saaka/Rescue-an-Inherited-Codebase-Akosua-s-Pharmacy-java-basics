package com.amalitech.pharmacy.domain;

import java.util.Optional;

public interface DispensingPolicy {
    String category();

    Optional<String> validationError(DispenseRequest request);
}
