// Console entry point: builds sample data, runs pharmacy operations, and displays their results.
package com.amalitech.pharmacy.app;

import com.amalitech.pharmacy.domain.Customer;
import com.amalitech.pharmacy.domain.CustomerType;
import com.amalitech.pharmacy.domain.Medication;
import com.amalitech.pharmacy.domain.OperationResult;
import com.amalitech.pharmacy.domain.OverTheCounterPolicy;
import com.amalitech.pharmacy.domain.Pharmacy;
import com.amalitech.pharmacy.domain.PrescriptionPolicy;

import java.math.BigDecimal;

public class PharmacyTracker {

    public static void main(String[] args) {
        // Create the pharmacy that owns the stock, customers, and transaction history.
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");

        // Register OTC stock and attach the rule set that applies to each medicine.
        Medication paracetamol = new Medication(
                "Paracetamol 500mg", new BigDecimal("12.50"), 100, new OverTheCounterPolicy());
        Medication amoxicillin = new Medication(
                "Amoxicillin 250mg", new BigDecimal("30.00"), 40, new PrescriptionPolicy());
        pharmacy.addMedication(paracetamol);
        pharmacy.addMedication(amoxicillin);
        // Register a walk-in customer and a patient who receives the registered-patient discount.
        Customer kofi = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        Customer ama = new Customer("Ama Owusu", CustomerType.REGISTERED, "0244 000 000");
        pharmacy.addCustomer(kofi);
        pharmacy.addCustomer(ama);

        // Demonstrate a valid OTC dispense, a prescribed dispense, and a stock replenishment.
        print(pharmacy.dispense(kofi, paracetamol, 2, null));
        print(pharmacy.dispense(ama, amoxicillin, 1, "Dr. Boateng"));
        print(pharmacy.restock(paracetamol, 50));

        // This request exceeds available stock; it must not change stock or create records.
        print(pharmacy.dispense(kofi, amoxicillin, 100, "Dr. Boateng"));

        // Show the resulting inventory and count of successful transactions.
        System.out.println(pharmacy.endOfDayReport());
    }

    // Display the result returned by a pharmacy operation.
    private static void print(OperationResult result) {
        System.out.println(result.message());
    }
}
