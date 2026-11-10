package com.amalitech.pharmacy.domain;

import java.util.Map;
import java.util.Objects;

public record DispenseRequest(int quantity, String prescriber, Map<String, String> additionalDetails) {
    public DispenseRequest {
        Objects.requireNonNull(additionalDetails, "Additional dispensing details are required");
        additionalDetails = Map.copyOf(additionalDetails);
    }

    public DispenseRequest(int quantity, String prescriber) {
        this(quantity, prescriber, Map.of());
    }
}
