# Akosua's Pharmacy — Stock and Dispensing Tracker

This Java 21 console application tracks pharmacy stock, customers, dispensing,
restocking, and an end-of-day stock report.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer to run the automated test suite

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
  policy can validate the prescriber and additional fields without adding
  category-specific branches to `Pharmacy`. The immutable transaction record
  retains the prescriber and those details for audit. This also addresses a
  gap in the starter, which printed the prescription but did not retain it in
  the transaction.
- **Currency uses `BigDecimal`.** This avoids binary floating-point error in
  prices and discounts. No extra rounding is introduced: the starter did not
  define a rounding rule, and totals preserve its arithmetic behavior. The
  display keeps familiar output such as `25.0`.

The diagnosis and initial design sketch are in [DIAGNOSIS.md](DIAGNOSIS.md).
The implementation uses an application and domain split rather than a separate
service package: `Pharmacy` is the aggregate root that owns the coordinated
domain operation. The policy interface and immutable dispense request make the
planned controlled category extensible while keeping the current behavior
focused and testable.

## Behavior and safety

The normal sample transactions preserve the starter's prices, registered
patient discount, stock changes, restock, and report. The attempted dispense of
100 amoxicillin is rejected because only 39 remain; it leaves stock at 39 and
does not create a transaction or customer-history entry.
