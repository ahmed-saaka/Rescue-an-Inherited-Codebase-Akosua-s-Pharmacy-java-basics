// Supported transaction kinds recorded by the pharmacy.
package com.amalitech.pharmacy.domain;

public enum TransactionType {
    // A successful medicine sale that reduces inventory.
    DISPENSE,

    // An inventory delivery that increases stock but is not a customer sale.
    RESTOCK
}
