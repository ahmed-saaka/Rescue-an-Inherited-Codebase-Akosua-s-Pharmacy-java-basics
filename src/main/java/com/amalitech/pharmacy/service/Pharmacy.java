package com.amalitech.pharmacy.service;

import com.amalitech.pharmacy.domain.Customer;
import com.amalitech.pharmacy.domain.CustomerType;
import com.amalitech.pharmacy.domain.DispenseRequest;
import com.amalitech.pharmacy.domain.Medication;
import com.amalitech.pharmacy.domain.Transaction;
import com.amalitech.pharmacy.domain.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class Pharmacy {
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
        medications.add(Objects.requireNonNull(medication, "Medication is required"));
    }

    public synchronized void addCustomer(Customer customer) {
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
        if (customer == null || !customers.contains(customer)) {
            return new OperationResult(false, "Cannot dispense to an unregistered pharmacy customer");
        }
        if (medication == null || !medications.contains(medication)) {
            return new OperationResult(false, "Cannot dispense medication not stocked by this pharmacy");
        }
        Objects.requireNonNull(request, "Dispense request is required");
        int quantity = request.quantity();
        if (quantity <= 0) {
            return new OperationResult(false, "Cannot dispense a non-positive quantity");
        }

        Optional<String> policyError = medication.dispensingPolicy().validationError(request);
        if (policyError.isPresent()) {
            return new OperationResult(false, "Cannot dispense " + medication.name()
                    + " " + policyError.get());
        }

        double total = medication.price() * quantity;
        if (customer.type() == CustomerType.REGISTERED) {
            total *= 0.95;
        }
        if (!Double.isFinite(total)) {
            return new OperationResult(false, "Cannot calculate a valid total for " + medication.name());
        }
        if (!medication.dispenseStock(quantity)) {
            return new OperationResult(false,
                    "Cannot dispense " + medication.name() + ": insufficient stock");
        }

        customer.recordDispense("Got " + quantity + " x " + medication.name());
        transactions.add(new Transaction(
                NEXT_TRANSACTION_ID.getAndIncrement(),
                customer.name(),
                medication.name(),
                quantity,
                total,
                TransactionType.DISPENSE,
                request.prescriber(),
                request.additionalDetails()));

        String message = "Dispensed " + quantity + " x " + medication.name() + " to " + customer.name();
        String prescriber = request.prescriber();
        if (prescriber != null && !prescriber.isBlank()) {
            message += " (prescribed by " + prescriber + ")";
        }
        message += " for GHS " + total;
        return new OperationResult(true, message);
    }

    public synchronized OperationResult restock(Medication medication, int quantity) {
        if (medication == null || !medications.contains(medication)) {
            return new OperationResult(false, "Cannot restock medication not stocked by this pharmacy");
        }
        if (!medication.restock(quantity)) {
            return new OperationResult(false,
                    "Cannot restock " + medication.name() + " with quantity " + quantity);
        }
        transactions.add(new Transaction(
                NEXT_TRANSACTION_ID.getAndIncrement(),
                "(supplier)",
                medication.name(),
                quantity,
                0,
                TransactionType.RESTOCK,
                null,
                Map.of()));
        return new OperationResult(true, "Restocked " + quantity + " x " + medication.name());
    }

    public synchronized List<Transaction> transactions() {
        return List.copyOf(transactions);
    }

    public synchronized void printReport() {
        System.out.println();
        System.out.println("=== " + name + " - end of day ===");
        System.out.println("Stock levels:");
        for (Medication medication : medications) {
            System.out.println("  " + medication.name() + " (" + medication.category()
                    + "): " + medication.stock() + " in stock");
        }
        System.out.println("Transactions today: " + transactions.size());
    }
}
