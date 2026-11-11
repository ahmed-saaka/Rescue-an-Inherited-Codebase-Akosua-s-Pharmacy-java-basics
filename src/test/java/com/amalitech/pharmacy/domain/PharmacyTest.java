// Regression tests for pharmacy behavior, stock integrity, audit records, and policy extensibility.
package com.amalitech.pharmacy.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PharmacyTest {

    // Protect the expected prices, patient discount, stock changes, and audit/history updates.
    @Test
    void preservesSuccessfulDispensingDiscountAndRestockBehavior() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication otc = new Medication(
                "Paracetamol 500mg", new BigDecimal("12.50"), 100, new OverTheCounterPolicy());
        Medication prescription = new Medication(
                "Amoxicillin 250mg", new BigDecimal("30.00"), 40, new PrescriptionPolicy());
        Customer walkIn = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        Customer registered = new Customer("Ama Owusu", CustomerType.REGISTERED);
        pharmacy.addMedication(otc);
        pharmacy.addMedication(prescription);
        pharmacy.addCustomer(walkIn);
        pharmacy.addCustomer(registered);

        assertTrue(pharmacy.dispense(walkIn, otc, 2, null).successful());
        assertTrue(pharmacy.dispense(registered, prescription, 1, "Dr. Boateng").successful());
        assertTrue(pharmacy.restock(otc, 50).successful());

        assertEquals(148, otc.stock());
        assertEquals(39, prescription.stock());
        assertEquals(3, pharmacy.transactions().size());
        assertEquals(new BigDecimal("25.00"), pharmacy.transactions().get(0).total());
        assertEquals(new BigDecimal("28.5000"), pharmacy.transactions().get(1).total());
        assertEquals(TransactionType.RESTOCK, pharmacy.transactions().get(2).type());
        assertEquals(List.of("Got 2 x Paracetamol 500mg"), walkIn.history());
    }

    // A failed dispense must leave every part of the pharmacy state unchanged.
    @Test
    void rejectsOverDispensingWithoutChangingStockOrRecordingSuccess() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "Amoxicillin 250mg", new BigDecimal("30.00"), 39, new PrescriptionPolicy());
        Customer customer = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        OperationResult result = pharmacy.dispense(customer, medication, 100, "Dr. Boateng");

        assertFalse(result.successful());
        assertEquals("Cannot dispense Amoxicillin 250mg: insufficient stock", result.message());
        assertEquals(39, medication.stock());
        assertTrue(customer.history().isEmpty());
        assertTrue(pharmacy.transactions().isEmpty());
    }

    // Guard against invalid quantities and integer overflow corrupting inventory.
    @Test
    void rejectsInvalidQuantitiesAndStockOverflow() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "Paracetamol 500mg", new BigDecimal("12.50"), 4, new OverTheCounterPolicy());
        Customer customer = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        assertFalse(pharmacy.dispense(customer, medication, 0, null).successful());
        assertFalse(pharmacy.dispense(customer, medication, -1, null).successful());
        assertFalse(pharmacy.restock(medication, 0).successful());
        assertFalse(pharmacy.restock(medication, -2).successful());
        assertFalse(pharmacy.restock(medication, Integer.MAX_VALUE).successful());
        assertThrows(IllegalArgumentException.class,
                () -> new Medication("Invalid stock", new BigDecimal("1.00"), -1,
                        new OverTheCounterPolicy()));
        assertEquals(4, medication.stock());
        assertTrue(pharmacy.transactions().isEmpty());
    }

    // Prescription validation must happen before stock, history, or transaction data changes.
    @Test
    void rejectsPrescriptionDispensingWithoutPrescriber() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "Amoxicillin 250mg", new BigDecimal("30.00"), 10, new PrescriptionPolicy());
        Customer customer = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        OperationResult missingPrescriber = pharmacy.dispense(customer, medication, 1, null);
        assertFalse(missingPrescriber.successful());
        assertEquals(
                "Cannot dispense Amoxicillin 250mg: A prescriber is required for prescription medicine.",
                missingPrescriber.message());
        assertFalse(pharmacy.dispense(customer, medication, 1, " ").successful());
        assertEquals(10, medication.stock());
        assertTrue(pharmacy.transactions().isEmpty());
        assertTrue(customer.history().isEmpty());
    }

    @Test
    void validatesRequestAndTransactionInvariantsAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new DispenseRequest(0, null));
        assertThrows(IllegalArgumentException.class, () -> new DispenseRequest(-1, null));
        assertThrows(IllegalArgumentException.class, () -> OperationResult.failure(""));
        assertThrows(IllegalArgumentException.class, () -> OperationResult.failure("  "));

        assertThrows(IllegalArgumentException.class, () -> new Transaction(
                1, "Kofi Mensah", "Medicine", 0, BigDecimal.ONE,
                TransactionType.DISPENSE, null, new DispenseRequest.NoCategoryDetails()));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(
                1, "Kofi Mensah", "Medicine", 1, BigDecimal.ONE.negate(),
                TransactionType.DISPENSE, null, new DispenseRequest.NoCategoryDetails()));
        assertThrows(NullPointerException.class, () -> new Transaction(
                1, "Kofi Mensah", "Medicine", 1, null,
                TransactionType.DISPENSE, null, new DispenseRequest.NoCategoryDetails()));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(
                1, "Kofi Mensah", null, 1, BigDecimal.ONE,
                TransactionType.DISPENSE, null, new DispenseRequest.NoCategoryDetails()));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(
                1, "Kofi Mensah", " ", 1, BigDecimal.ONE,
                TransactionType.DISPENSE, null, new DispenseRequest.NoCategoryDetails()));
    }

    @Test
    void trimsPrescriberBeforeValidationAndAuditRecording() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "Amoxicillin 250mg", new BigDecimal("30.00"), 2, new PrescriptionPolicy());
        Customer customer = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        assertTrue(pharmacy.dispense(customer, medication, 1, "  Dr. Boateng  ").successful());

        assertEquals("Dr. Boateng", pharmacy.transactions().get(0).prescriber());
    }

    // BigDecimal arithmetic should preserve very large exact totals without primitive overflow.
    @Test
    void preservesExactLargeTotalsWithoutOverflow() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "High value medicine", new BigDecimal("1E+1000"), 2, new OverTheCounterPolicy());
        Customer customer = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        OperationResult result = pharmacy.dispense(customer, medication, 2, null);

        assertTrue(result.successful());
        assertEquals(0, medication.stock());
        assertEquals(new BigDecimal("2E+1000"), pharmacy.transactions().get(0).total());
    }

    // New policies can validate typed category details and retain them in the transaction.
    @Test
    void allowsNewPoliciesAndKeepsPrescriberAndCategoryDetailsInAuditRecord() {
        DispensingPolicy controlledPolicy = new DispensingPolicy() {
            @Override
            public String category() {
                return "CONTROLLED";
            }

            @Override
            public Optional<String> validationError(DispenseRequest request) {
                if (request.prescriber() == null || request.prescriber().isBlank()) {
                    return Optional.of("A prescriber is required.");
                }
                if (!(request.categoryDetails() instanceof DispenseRequest.ControlledSubstanceDetails)) {
                    return Optional.of("A permit is required.");
                }
                return Optional.empty();
            }
        };
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication("Controlled medicine", new BigDecimal("5.00"), 3, controlledPolicy);
        Customer customer = new Customer("Ama Owusu", CustomerType.REGISTERED);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        assertFalse(pharmacy.dispense(customer, medication,
                new DispenseRequest(1, "Dr. Boateng")).successful());
        assertTrue(pharmacy.dispense(customer, medication,
                new DispenseRequest(1, "Dr. Boateng",
                        new DispenseRequest.ControlledSubstanceDetails("P-42"))).successful());

        var transaction = pharmacy.transactions().get(0);
        assertEquals("Dr. Boateng", transaction.prescriber());
        assertEquals(new DispenseRequest.ControlledSubstanceDetails("P-42"), transaction.categoryDetails());
    }

    // Public collection accessors must not let callers mutate the pharmacy's internal lists.
    @Test
    void collectionAccessorsDoNotExposeMutableInternalState() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "Paracetamol 500mg", new BigDecimal("12.50"), 2, new OverTheCounterPolicy());
        pharmacy.addMedication(medication);

        assertThrows(UnsupportedOperationException.class,
                () -> pharmacy.medications().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> pharmacy.transactions().clear());
    }

    // An optional prescriber should not appear in an OTC confirmation when it is not required.
    @Test
    void ignoresOptionalPrescriberInOverTheCounterConfirmation() {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");
        Medication medication = new Medication(
                "Paracetamol 500mg", new BigDecimal("12.50"), 2, new OverTheCounterPolicy());
        Customer customer = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        pharmacy.addMedication(medication);
        pharmacy.addCustomer(customer);

        OperationResult result = pharmacy.dispense(customer, medication, 1, "Dr. Optional");

        assertEquals("Dispensed 1 x Paracetamol 500mg to Kofi Mensah for GHS 12.5",
                result.message());
    }
}
