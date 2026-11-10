# Akosua's Pharmacy: Diagnosis and Design Sketch

## Diagnosis of the starter

The starter combines its entry point, models, dispensing, stock updates,
transaction recording, and reporting in one source file. More importantly,
rules are not enforced at a single boundary before mutable state changes.

| Priority | Root cause | Business risk |
|---|---|---|
| **Critical** | Dispensing subtracts a requested quantity without first checking stock. The sample asks for 100 amoxicillin when only 39 remain. | Inventory can go negative, so staff cannot trust availability reports when making dispensing decisions. For a pharmacy, unreliable inventory can affect patient safety. |
| **High** | Stock and other model fields are public; restocking accepts unrestricted quantities. | Callers can bypass the intended rules or reduce stock with a negative restock, creating invalid inventory. |
| **High** | Medicine category and customer type are unrestricted strings, and all dispensing rules sit in one conditional. | Typos can skip behavior, and adding controlled substances means changing the same logic used by OTC and prescription items, risking regressions. |
| **High** | There are no automated tests in the starter. | Changes to stock, discount, or prescription behavior have no repeatable regression check. |
| **Medium** | Dispense validation does not reject non-positive quantities, blank prescribers, or objects not registered with the pharmacy. | Invalid operations can be accepted or recorded as successful activity. |
| **Medium** | Customer history, pharmacy collections, and the transaction list are mutable outside the pharmacy operations. | Records can be altered or removed without the business operation that should govern them. |
| **Medium** | Models, rules, reporting, and the entry point are tightly coupled in one file. | A change to one behavior is harder to isolate, test, and review, increasing maintenance risk. |
| **Low** | Monetary amounts use `double`. | Binary floating-point results may not represent currency exactly and can make totals confusing. |

### Most dangerous issue

Negative or inaccurate stock is the highest risk. Staff use the stock count to
decide whether a medicine is available. If that count is wrong, decisions based
on the report are unreliable. The stock invariant must be enforced before an
operation succeeds, not merely reported after the count has become invalid.

## Initial design sketch

```text
                 +----------------------+
                 | PharmacyTracker (CLI) |
                 +----------+-----------+
                            |
                 +----------v-----------+
                 | Pharmacy (domain root)|
                 +----+-------+------+--+
                      |       |      |
             +--------v--+ +--v-----+ +v----------------+
             | Medication| |Customer| | Transaction log |
             | private   | | history| | immutable       |
             | stock     | +--------+ +-----------------+
             +-----+-----+
                   |
            DispensingPolicy
              /          \
           OTC       Prescription
                         ...
                    Controlled
```

The policy boundary is the extension point: each medicine category implements
its own validation. A controlled policy can validate a prescriber and any
additional dispense-request details without adding a third category branch to
the pharmacy's stock/recording workflow.

## Design notes and changes from the initial sketch

The implementation uses application and domain packages rather than the
single `PharmacyService` component drawn in the initial sketch:

- `app` contains the console entry point.
- `domain` contains `Medication`, `Customer`, immutable `Transaction` and
  `DispenseRequest` records, enums, dispensing policies, and `Pharmacy`.
- `Pharmacy` is the aggregate root that coordinates registered
  customers/medicines, validates requests, and records successful operations.

Keeping the pharmacy operation in the domain package allows stock and history
mutation to remain package-private. External callers cannot change inventory or
customer history without going through the operation that records the matching
transaction. The policy receives a request with an optional details map. A
future controlled policy can require fields such as a permit number, and the
transaction retains both the prescriber and the supplied details.

An additional business-focused improvement is keeping the prescriber and
additional dispense details in an immutable transaction record. The starter
printed the prescriber for a prescription dispense but did not retain it in the
transaction, weakening the pharmacy's audit trail.

Prices and totals use `BigDecimal` to avoid binary floating-point artifacts.
No new rounding rule is applied because the starter did not define one; output
formatting retains the familiar sample amounts.

## Implementation chronology

The initial repository commit (`e2907cb`) contains both `DIAGNOSIS.md` and the
starter source already modified with a stock-safety guard. The diagnosis
therefore did not precede the first code change, and the commit cannot be
presented as proof of that ordering. The broader rebuild rejects
over-dispensing and invalid stock updates; a failed dispense does not change
stock, customer history, or transactions.
