package com.amalitech.pharmacy.app;

import com.amalitech.pharmacy.domain.Customer;
import com.amalitech.pharmacy.domain.CustomerType;
import com.amalitech.pharmacy.domain.Medication;
import com.amalitech.pharmacy.domain.OverTheCounterPolicy;
import com.amalitech.pharmacy.domain.PrescriptionPolicy;
import com.amalitech.pharmacy.service.OperationResult;
import com.amalitech.pharmacy.service.Pharmacy;

public class PharmacyTracker {

    public static void main(String[] args) {
        Pharmacy pharmacy = new Pharmacy("Akosua's Pharmacy");

        Medication paracetamol = new Medication(
                "Paracetamol 500mg", 12.50, 100, new OverTheCounterPolicy());
        Medication amoxicillin = new Medication(
                "Amoxicillin 250mg", 30.00, 40, new PrescriptionPolicy());
        pharmacy.addMedication(paracetamol);
        pharmacy.addMedication(amoxicillin);

        Customer kofi = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        Customer ama = new Customer("Ama Owusu", CustomerType.REGISTERED, "0244 000 000");
        pharmacy.addCustomer(kofi);
        pharmacy.addCustomer(ama);

        print(pharmacy.dispense(kofi, paracetamol, 2, null));
        print(pharmacy.dispense(ama, amoxicillin, 1, "Dr. Boateng"));
        print(pharmacy.restock(paracetamol, 50));
        print(pharmacy.dispense(kofi, amoxicillin, 100, "Dr. Boateng"));

        pharmacy.printReport();
    }

    private static void print(OperationResult result) {
        System.out.println(result.message());
    }
}
