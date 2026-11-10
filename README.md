# Akosua's Pharmacy — Stock and Dispensing Tracker

This Java 21 console application tracks pharmacy stock, customers, dispensing,
restocking, and an end-of-day stock report.

## Run

With Maven:

```shell
mvn test
mvn exec:java -Dexec.mainClass=com.amalitech.pharmacy.app.PharmacyTracker
```

Or with a JDK 21:

```powershell
$sources = Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object FullName
javac -d target\classes $sources
java -cp target\classes com.amalitech.pharmacy.app.PharmacyTracker
```

## Design decisions

- **Stock belongs to `Medication`.** The stock field is private; synchronized
  dispense/restock operations reject non-positive quantities, over-dispensing,
  and integer overflow. Failed operations do not change inventory.
- **Dispensing behavior is a policy.** `DispensingPolicy` separates a medicine's
  requirements from pharmacy coordination. OTC and prescription policies are
  independent implementations. `CustomerType` and `TransactionType` are enums
  instead of unchecked strings.
- **`Pharmacy` coordinates operations.** It checks membership and policy before
  changing stock, then updates customer history and records a transaction only
  on success. Collection accessors return immutable snapshots.
- **A dispense request carries optional extra details.** A controlled-medicine
  policy can validate the prescriber and additional fields without adding
  category-specific branches to `Pharmacy`. The immutable transaction record
  retains the prescriber and those details for audit. This also addresses a
  gap in the starter, which printed the prescription but did not retain it in
  the transaction.
- **Prices remain `double` for behavior compatibility.** The starter used
  floating-point arithmetic and printed results such as `25.0`; switching to
  decimal currency arithmetic needs an explicit rounding/display decision and
  is not mixed into this safety-focused rebuild.

The diagnosis and initial design sketch are in [DIAGNOSIS.md](DIAGNOSIS.md).
The implementation uses a more concrete split into application, domain, and
service packages than the initial sketch; the policy interface and immutable
dispense request make the planned controlled category extensible while keeping
the current behavior focused and testable.

## Behavior and safety

The normal sample transactions preserve the starter's prices, registered
patient discount, stock changes, restock, and report. The attempted dispense of
100 amoxicillin is rejected because only 39 remain; it leaves stock at 39 and
does not create a transaction or customer-history entry.
