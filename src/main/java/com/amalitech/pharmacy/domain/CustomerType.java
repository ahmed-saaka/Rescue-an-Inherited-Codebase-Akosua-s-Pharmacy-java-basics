// Allowed customer categories used for pharmacy rules such as registered-patient discounts.
package com.amalitech.pharmacy.domain;

public enum CustomerType {
    // Customers without a registered account pay the standard medication price.
    WALK_IN,

    // Registered patients are eligible for the discount applied by Pharmacy.
    REGISTERED
}
