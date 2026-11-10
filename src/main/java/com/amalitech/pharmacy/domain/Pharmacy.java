// Pharmacy aggregate: validates operations and coordinates stock, customer history, and transaction records.
package com.amalitech.pharmacy.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

// The starter spread mutable operations across one file; this aggregate coordinates each business change.
public final class Pharmacy {
    private static final BigDecimal REGISTERED_PATIENT_DISCOUNT = new BigDecimal("0.95");
    private static final AtomicInteger NEXT_TRANSACTION_ID = new AtomicInteger(1);

    private final String name;
    private final List<Medication> medications = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public Pharmacy(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pharmacy name cannot be blank");
        }
        this.name = name;
    }

    public String name() {
        return name;
    }

    public synchronized void addMedication(Medication medication) {
        // Keep the registered inventory list private and reject null entries.
        medications.add(Objects.requireNonNull(medication, "Medication is required"));
    }

    public synchronized void addCustomer(Customer customer) {
        // Membership checks below only accept customers registered with this pharmacy.
        customers.add(Objects.requireNonNull(customer, "Customer is required"));
    }

    public synchronized List<Medication> medications() {
        return List.copyOf(medications);
    }

    public synchronized List<Customer> customers() {
        return List.copyOf(customers);
    }

    public synchronized OperationResult dispense(
            Customer customer, Medication medication, int quantity, String prescriber) {
        return dispense(customer, medication, new DispenseRequest(quantity, prescriber));
    }

    public synchronized OperationResult dispense(
            Customer customer, Medication medication, DispenseRequest request) {
        // Refuse unknown objects so an operation cannot be attributed to another pharmacy.
        if (customer == null || !customers.contains(customer)) {
            return new OperationResult(false, "Cannot dispense to an unregistered pharmacy customer");
        }
        if (medication == null || !medications.contains(medication)) {
            return new OperationResult(false, "Cannot dispense medication not stocked by this pharmacy");
        }
        Objects.requireNonNull(request, "Dispense request is required");
        if (request.quantity() <= 0) {
            return new OperationResult(false, "Cannot dispense a non-positive quantity");
        }

        // The starter used string branches here; policies keep new medicine rules out of the shared workflow.
        Optional<String> policyError = medication.dispensingPolicy().validationError(request);
        if (policyError.isPresent()) {
            return new OperationResult(false,
                    "Cannot dispense " + medication.name() + " " + policyError.get());
        }

        // The starter deducted stock before checking availability; reject before changing any recorded state.
        // Calculate the charge first, applying the existing discount only to registered patients.
        BigDecimal total = medication.price().multiply(BigDecimal.valueOf(request.quantity()));
        if (customer.type() == CustomerType.REGISTERED) {
            total = total.multiply(REGISTERED_PATIENT_DISCOUNT);
        }
        if (!medication.dispenseStock(request.quantity())) {
            return new OperationResult(false,
                    "Cannot dispense " + medication.name() + ": insufficient stock");
        }

        // History and audit records are written only after stock successfully changes.
        customer.recordDispense("Got " + request.quantity() + " x " + medication.name());
        transactions.add(new Transaction(
                NEXT_TRANSACTION_ID.getAndIncrement(),
                customer.name(),
                medication.name(),
                request.quantity(),
                total,
                TransactionType.DISPENSE,
                request.prescriber(),
                request.additionalDetails()));

        String message = "Dispensed " + request.quantity() + " x " + medication.name()
                + " to " + customer.name();
        if (medication.dispensingPolicy().prescriberRequired()) {
            message += " (prescribed by " + request.prescriber() + ")";
        }
        message += " for GHS " + formatAmount(total);
        return new OperationResult(true, message);
    }

    public synchronized OperationResult restock(Medication medication, int quantity) {
        // Only this pharmacy's medication can be restocked through this operation.
        if (medication == null || !medications.contains(medication)) {
            return new OperationResult(false, "Cannot restock medication not stocked by this pharmacy");
        }
        if (!medication.restock(quantity)) {
            return new OperationResult(false,
                    "Cannot restock " + medication.name() + " with quantity " + quantity);
        }
        // Record the restock only after the stock update has succeeded.
        transactions.add(new Transaction(
                NEXT_TRANSACTION_ID.getAndIncrement(),
                "(supplier)",
                medication.name(),
                quantity,
                BigDecimal.ZERO,
                TransactionType.RESTOCK,
                null,
                Map.of()));
        return new OperationResult(true, "Restocked " + quantity + " x " + medication.name());
    }

    public synchronized List<Transaction> transactions() {
        // The starter returned its mutable internal list; callers now receive a read-only snapshot.
        return List.copyOf(transactions);
    }

    public synchronized String endOfDayReport() {
        // Build report text as data; the command-line layer decides where to print it.
        StringBuilder report = new StringBuilder()
                .append('\n')
                .append("=== ").append(name).append(" - end of day ===\n")
                .append("Stock levels:\n");
        for (Medication medication : medications) {
            report.append("  ").append(medication.name())
                    .append(" (").append(medication.category()).append("): ")
                    .append(medication.stock()).append(" in stock\n");
        }
        return report.append("Transactions today: ").append(transactions.size()).toString();
    }

    private static String formatAmount(BigDecimal amount) {
        String formatted = amount.stripTrailingZeros().toPlainString();
        return formatted.contains(".") ? formatted : formatted + ".0";
    }
}
