# Akosua's Pharmacy — Stock and Dispensing Tracker

This Java 21 console application tracks pharmacy stock, customers, dispensing,
restocking, and an end-of-day stock report.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer to run the automated test suite

The [pharmacy domain test package](src/test/java/com/amalitech/pharmacy/domain/)
contains `PharmacyTest`, which asserts dispense and restock state changes,
discounted and exact totals, and unchanged stock/history/transactions after
rejected operations.

## Run

With Maven:

```shell
mvn test
mvn exec:java -Dexec.mainClass=com.amalitech.pharmacy.app.PharmacyTracker
```

Or compile and run with a JDK 21.

On Windows PowerShell:

```powershell
$sources = Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object FullName
javac -d target\classes $sources
java -cp target\classes com.amalitech.pharmacy.app.PharmacyTracker
```

On POSIX shells (Linux/macOS):

```sh
mkdir -p target/classes
find src/main/java -name '*.java' -print0 | xargs -0 javac -d target/classes
java -cp target/classes com.amalitech.pharmacy.app.PharmacyTracker
```

## Design decisions

- **Stock belongs to `Medication`.** The stock field is private; synchronized
  dispense/restock operations are package-private and called by the `Pharmacy`
  aggregate, so external callers cannot change inventory without creating the
  corresponding pharmacy transaction. Invalid quantities, over-dispensing, and
  integer overflow are rejected.
- **Dispensing behavior is a policy.** `DispensingPolicy` separates a medicine's
  requirements from pharmacy coordination. OTC and prescription policies are
  independent implementations. `CustomerType` and `TransactionType` are enums
  instead of unchecked strings.
- **`Pharmacy` is the domain aggregate root.** It checks membership and policy
  before changing stock, then updates customer history and records a transaction
  only on success. Stock/history mutation is not exposed publicly; collection
  accessors return immutable snapshots. The CLI owns console output, while the
  pharmacy provides a report string.
- **A dispense request carries optional extra details.** A controlled-medicine
  policy can validate the prescriber and typed permit details without adding
  category-specific branches to `Pharmacy`. The immutable transaction record
  retains the prescriber and those details for audit. This also addresses a
  gap in the starter, which printed the prescription but did not retain it in
  the transaction.
- **Currency uses `BigDecimal`.** This avoids binary floating-point error in
  prices and discounts. No extra rounding is introduced: the starter did not
  define a rounding rule, and totals preserve its arithmetic behavior. The
  display keeps familiar output such as `25.0`.

### Adding a controlled substance

Implement a `DispensingPolicy` that validates the prescriber and requires
`DispenseRequest.ControlledSubstanceDetails`. Register the medication with that
policy, then submit the typed permit detail with the request:

```java
Medication controlled = new Medication(
        "Controlled medicine", new BigDecimal("5.00"), 3, controlledPolicy);
pharmacy.addMedication(controlled);
pharmacy.dispense(customer, controlled,
        new DispenseRequest(1, "Dr. Boateng",
                new DispenseRequest.ControlledSubstanceDetails("P-42")));
```

The policy can reject missing or invalid permit details without adding a
controlled-category branch to `Pharmacy`; successful requests retain the typed
details in the transaction audit record.

The diagnosis and initial design sketch are in [DIAGNOSIS.md](DIAGNOSIS.md)
and [design-sketch.png](design-sketch.png).
The implementation uses an application and domain split rather than a separate
service package: `Pharmacy` is the aggregate root that owns the coordinated
domain operation, and the CLI entry point lives in
`com.amalitech.pharmacy.app`. The policy interface and typed immutable dispense
request keep category rules extensible while making request details explicit.

## Behavior and safety

The normal sample transactions preserve the starter's prices, registered
patient discount, stock changes, restock, and report. The attempted dispense of
100 amoxicillin is rejected because only 39 remain; it leaves stock at 39 and
does not create a transaction or customer-history entry.

Expected CLI output:

```text
Dispensed 2 x Paracetamol 500mg to Kofi Mensah for GHS 25.0
Dispensed 1 x Amoxicillin 250mg to Ama Owusu (prescribed by Dr. Boateng) for GHS 28.5
Restocked 50 x Paracetamol 500mg
Cannot dispense Amoxicillin 250mg: insufficient stock

=== Akosua's Pharmacy - end of day ===
Stock levels:
  Paracetamol 500mg (OTC): 148 in stock
  Amoxicillin 250mg (PRESCRIPTION): 39 in stock
Transactions today: 3
```
