KudiKakra — Parser Specification

1. Purpose

The parser system converts financial notifications into normalized transaction records.

The parser must be deterministic wherever possible.

Do not use AI for basic transaction extraction in the MVP.

---

2. Parser Pipeline

Android Notification
        ↓
NotificationEvent
        ↓
Source Detection
        ↓
Financial Detection
        ↓
Provider Parser
        ↓
Parse Result
        ↓
Validation
        ↓
Confidence
        ↓
Transaction

---

3. NotificationEvent

A notification event should contain:

packageName
title
text
timestamp

The parser should work from this event.

Do not make parsers dependent on Android UI components.

---

4. FinancialParser Interface

All provider parsers should follow the same conceptual interface:

interface FinancialParser {
    fun canHandle(event: NotificationEvent): Boolean
    fun parse(event: NotificationEvent): ParseResult
}

The exact implementation may differ.

---

5. ParseResult

A parse result should contain:

success
transaction
confidence
reason

Example:

success = true
confidence = HIGH
transaction.type = EXPENSE
transaction.amount = 25.00
transaction.currency = GHS

---

6. Transaction Types

Every parsed transaction must have one primary type:

EXPENSE
INCOME
TRANSFER
WITHDRAWAL
UNKNOWN

EXPENSE

Money genuinely spent.

Examples:

Paid GH₵20 to merchant
Purchased item for GH₵50
Paid GH₵15 for service

These normally count toward daily spending.

INCOME

Money received.

Examples:

Received GH₵500 from John
Salary GH₵2,000 received
Refund GH₵30 received

These do NOT count as expenditure.

TRANSFER

Money moved from one account/person to another where the transaction is not clearly a purchase.

Examples:

Sent GH₵500 to another person
Transferred GH₵1,000 to another account

Do not automatically classify every transfer as expenditure.

WITHDRAWAL

Money converted from digital account funds to cash.

Example:

Cash withdrawal GH₵800

Do not automatically count withdrawals as expenditure.

UNKNOWN

The parser cannot confidently determine what happened.

Unknown transactions must not automatically increase expenditure.

---

7. Direction

The parser should explicitly identify money direction.

IN
OUT
UNKNOWN

This is separate from transaction type.

Example:

Direction: IN
Type: INCOME

or:

Direction: OUT
Type: EXPENSE

or:

Direction: OUT
Type: WITHDRAWAL

This prevents the common mistake of assuming:

money left account = money spent

---

8. Important Semantic Distinction

The parser should distinguish:

MONEY RECEIVED

from:

MONEY SENT

and:

MONEY SPENT

These are not automatically the same thing.

For example:

Received GH₵1,000

means:

INCOME

while:

Sent GH₵1,000 to another person

means:

TRANSFER

unless there is evidence that the payment represents a purchase.

---

9. Extraction Fields

Where available, extract:

amount
currency
type
direction
merchant
reference
timestamp
source
confidence

Do not require every field to be present.

The amount and transaction direction/type are more important than merchant information.

---

10. Amount Extraction

The parser should support Ghanaian currency representations such as:

GH₵25
GH₵25.00
GHS 25
GHS25
25.00 GHS

Use deterministic parsing and regular expressions where appropriate.

Do not rely on a fixed exact notification sentence.

Notification wording may change.

---

11. Expense Keywords

Potential indicators include words such as:

paid
purchase
payment
debit
spent
bought
merchant

These are only indicators.

Do not classify a transaction based on a single keyword when the surrounding message contradicts it.

---

12. Income Keywords

Potential indicators include:

received
credited
deposit
salary
refund
cash received

Again, use the overall notification context.

---

13. Transfer Keywords

Potential indicators include:

sent
transfer
transferred
send money

A transfer should remain a transfer unless the notification clearly indicates that it was a purchase/payment.

---

14. Withdrawal Keywords

Potential indicators include:

withdrawal
withdrawn
cash out
cash-out
ATM
cash

Do not automatically count these as expenditure.

---

15. Large Withdrawal Rule

A large withdrawal must not automatically become a large expense.

Example:

Withdrawal: GH₵1,000

should produce:

type = WITHDRAWAL
excludedFromSpending = true

The user can later manually record actual cash expenses if the product supports this.

---

16. Parser Confidence

HIGH

Use when:

- provider is confidently identified
- amount is clear
- direction is clear
- transaction type is clear

MEDIUM

Use when:

- amount is clear
- source is likely correct
- transaction type has some ambiguity

LOW

Use when:

- amount is unclear
- source is unclear
- direction is unclear
- message appears financial but cannot be reliably classified

---

17. Parser Registry

Do not write:

if MTN ...
else if GCB ...
else if GhanaPay ...
else if Telecel ...
else ...

throughout the application.

Instead:

ParserRegistry
    ↓
registered parsers
    ↓
find parser that canHandle(event)
    ↓
parse

Possible initial parsers:

MTNParser
GCBParser
GenericFinancialParser

Add more only when needed.

---

18. Generic Parser

A generic parser may be used as a fallback.

It should be conservative.

If it cannot confidently classify the notification:

UNKNOWN

is better than incorrectly adding money to expenditure.

False expenditure is more harmful to the product than missing one transaction.

---

19. Provider Parser Versions

Notification formats can change.

Avoid assuming a provider has one permanent format.

The architecture should allow:

MTNParserV1
MTNParserV2
MTNFallbackParser

without changing the rest of the application.

Do not create versions prematurely. Introduce them only when real notification variations require them.

---

20. Parser Testing

Every parser should have unit tests using representative notification examples.

Example:

Input:
"You have paid GH₵25.00 to Merchant X..."

Expected:
type = EXPENSE
direction = OUT
amount = 25.00
currency = GHS

Another:

Input:
"You have received GH₵500 from John..."

Expected:
type = INCOME
direction = IN
amount = 500.00
currency = GHS

Another:

Input:
"You have withdrawn GH₵800..."

Expected:
type = WITHDRAWAL
direction = OUT
amount = 800.00
excludedFromSpending = true

---

21. Parser Playground

During development, create a screen where a developer can paste a notification and run the parser.

Display:

Source
Amount
Currency
Direction
Type
Merchant
Reference
Confidence
Excluded from spending

This is one of the most important development tools for this project.

---

22. Parser Safety Principle

The parser must prefer:

«"I don't know."»

over:

«"This is definitely spending."»

Incorrect expenditure data damages the main purpose of KudiKakra.

Conservative classification is therefore preferred over aggressive classification.

---

23. Normalized Output

Regardless of provider, the rest of the application should receive the same normalized model.

Transaction
{
    amount,
    currency,
    type,
    direction,
    merchant,
    reference,
    source,
    timestamp,
    confidence,
    excludedFromSpending
}

The dashboard, budget engine and widget should never need to know how MTN, GCB or another provider formats its notification.