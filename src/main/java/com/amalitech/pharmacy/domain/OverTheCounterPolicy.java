package com.amalitech.pharmacy.domain;

import java.util.Optional;

public final class OverTheCounterPolicy implements DispensingPolicy {
    @Override
    public String category() {
        return "OTC";
    }

    @Override
    public Optional<String> validationError(DispenseRequest request) {
        return Optional.empty();
    }
}
