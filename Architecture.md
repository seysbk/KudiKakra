# KudiKakra — Architecture

## 1. Architecture Goal

Build a simple native Android application that automatically detects supported financial transactions, normalizes them into a common format, stores them locally, calculates daily expenditure and displays the result through the application, notifications and a home-screen widget.

The MVP must remain local-first.

Implementation priority:

Build and verify the smallest useful flow before adding automatic detection:

1. Manual transaction entry
2. Local storage
3. Daily spending calculation
4. Daily spending plan
5. Notification-based detection for one provider

Do not add multiple providers, widgets, spending notifications, advanced analytics, AI, cloud backup, or synchronization until the manual spending flow is reliable.

---

## 2. Technology Stack

Android

- Kotlin
- Jetpack Compose
- Android SDK
- Android Studio for building/running/testing

Local Data

- Room
- SQLite underneath Room

Transaction Detection

- NotificationListenerService

Widget

- Jetpack Glance

Architecture

- MVVM-style presentation
- Repository pattern
- Clear separation between notification ingestion, parsing, data storage and business logic

Network

No network dependency for the core MVP.

---

## 3. High-Level Architecture

```text
                         KudiKakra
                            │
             ┌──────────────┴──────────────┐
             │                             │
        Compose UI                   Notification
             │                        Listener Service
             │                             │
             │                      Raw Notification
             │                             │
             │                       Source Detector
             │                             │
             │                       Parser Registry
             │                             │
             │            ┌────────────────┼────────────────┐
             │            │                │                │
             │        MTNParser        GCBParser       OtherParser
             │            │                │                │
             │            └────────────────┼────────────────┘
             │                             │
             │                       Parsed Transaction
             │                             │
             │                     Validation / Confidence
             │                             │
             │                        Transaction Engine
             │                             │
             │                            Room
             │                     ┌───────┴────────┐
             │                     │                │
             │               Transactions       Budgets
             │                     │                │
             │                     └───────┬────────┘
             │                             │
             │                       Budget Engine
             │                             │
             ├───────────────┬─────────────┼──────────────┐
             │               │             │              │
         Dashboard      Notifications   Widget        History
```

---

## 4. Core Data Flow

```text
Notification
     ↓
NotificationListenerService
     ↓
Notification Event
     ↓
Source Detector
     ↓
Is source selected?
     ↓
Financial Transaction Detector
     ↓
Parser Registry
     ↓
Provider Parser
     ↓
Normalized Transaction
     ↓
Validation
     ↓
Confidence Check
     ↓
Transaction Engine
     ↓
Room
     ↓
Budget Engine
     ↓
Dashboard + Notification + Widget
```

---

## 5. NotificationListenerService

The notification listener is responsible only for receiving Android notifications.

It should not contain business logic.

Responsibilities:

- receive notification
- extract package name
- extract title
- extract notification text
- extract timestamp
- create a lightweight notification event
- pass the event to the source detector/parser pipeline

Do NOT put MTN/GCB parsing logic directly inside the service.

Conceptually:

```text
NotificationListenerService
        ↓
NotificationEvent
        ↓
TransactionProcessor
```

---

## 6. Notification Event

Use a model similar to:

```text
NotificationEvent
- packageName
- title
- text
- timestamp
```

Raw notification information should be short-lived where possible.

Do not automatically store every notification.

Only relevant normalized transaction data should enter the database.

---

## 7. Source Detection

The source detector determines whether a notification could belong to a financial service selected by the user.

It can use:

- Android package name
- notification title
- known provider identifiers
- configured user sources

Example:

```text
Notification
    ↓
Package/title
    ↓
Possible MTN?
    ↓
MTNParser
```

Source detection should be separate from parsing.

---

## 8. Parser Registry

The parser registry prevents the application from becoming a giant "if/else" chain.

Conceptually:

```text
ParserRegistry
    ├── MTNParser
    ├── GCBParser
    ├── GhanaPayParser
    ├── TelecelParser
    └── GenericFinancialParser
```

Each parser implements the same interface.

Example:

```kotlin
interface FinancialParser {
    fun canHandle(event: NotificationEvent): Boolean
    fun parse(event: NotificationEvent): ParseResult
}
```

The exact implementation can be adjusted during development.

---

## 9. Transaction Model

The normalized transaction should contain information such as:

```text
Transaction
- id
- source
- amount
- currency
- type
- merchant
- reference
- timestamp
- confidence
- category
- excludedFromSpending
- createdAt
```

Possible transaction types:

```text
EXPENSE
INCOME
TRANSFER
WITHDRAWAL
UNKNOWN
```

---

## 10. Important Transaction Rule

The parser MUST distinguish between:

MONEY OUT

and

MONEY IN

Do not simply interpret every financial notification as expenditure.

Example:

"You have received GH₵500 from John"

should produce:

type = INCOME

not:

type = EXPENSE

Similarly:

"You have withdrawn GH₵800"

should produce:

type = WITHDRAWAL

not automatically:

type = EXPENSE

And:

"You paid GH₵35 to Merchant X"

should produce:

type = EXPENSE

---

## 11. Spending Calculation

The budget engine should calculate expenditure using only transactions that qualify as spending.

Conceptually:

Today's expenditure =
SUM(
    transactions
    WHERE date = today
    AND type = EXPENSE
    AND excludedFromSpending = false
)

Income does not reduce or increase today's expenditure.

Withdrawals do not automatically increase expenditure.

Transfers should be excluded unless they are explicitly classified as spending.

---

## 12. Large Cash Withdrawal Edge Case

Example:

Received GH₵1,000
        ↓
Withdraw GH₵800 cash
        ↓
Spent GH₵50 on food

KudiKakra should ideally report:

Today's spending: GH₵50

not:

Today's spending: GH₵850

The GH₵800 withdrawal is money movement, not necessarily consumption.

Future versions may allow the user to manually record cash spending after withdrawing money.

---

## 13. Confidence System

Parsers should return a confidence level.

Possible values:

HIGH
MEDIUM
LOW

Example:

HIGH

means the parser confidently identified:

- source
- amount
- direction
- transaction type

High-confidence transactions can be automatically saved.

Medium-confidence transactions may be presented for confirmation.

Low-confidence transactions should generally not be automatically treated as expenditure.

---

## 14. Duplicate Prevention

The same transaction must not be counted multiple times.

The transaction engine should eventually support deduplication using available information such as:

- source
- amount
- timestamp
- reference
- transaction identifier

Do not assume timestamp alone is enough.

---

## 15. Budget Engine

The budget engine is responsible for:

- retrieving today's spending plan
- calculating today's expenditure
- calculating remaining amount
- calculating percentage used
- determining whether the user is approaching the plan
- determining whether the plan has been exceeded

Example:

Daily plan = GH₵60
Expenditure = GH₵48

Remaining = GH₵12
Percentage = 80%

The budget engine should contain business rules, not UI code.

---

## 16. Repository Layer

The UI should not directly access Room DAOs.

Use:

Composable
    ↓
ViewModel
    ↓
Repository
    ↓
DAO
    ↓
Room

For example:

DashboardViewModel
        ↓
TransactionRepository
        ↓
TransactionDao
        ↓
Room

This keeps the application easier to modify.

---

## 17. Widget Architecture

The widget reads the relevant calculated state from the local application data.

Conceptually:

Room
 ↓
Spending Repository
 ↓
Today's Spending State
 ↓
Glance Widget

Initial widget:

TODAY

GH₵48 / GH₵60

GH₵12 remaining

The widget should not contain complicated financial calculations itself.

---

## 18. Android Notifications

The notification engine should receive spending state from the budget engine.

Example:

Budget:
GH₵60

Spent:
GH₵52

Remaining:
GH₵8

State:
APPROACHING_LIMIT

The notification engine decides whether to notify.

Do not send repeated notifications for every transaction.

---

## 19. Development Architecture

During development, include a developer/test pathway.

Recommended:

Parser Playground
Notification Inspector
Test Transaction
Test Budget

This allows parser development without constantly making real financial transactions.

---

## 20. No Backend

Do not add:

- Django
- REST API
- PostgreSQL
- Firebase
- Supabase
- cloud authentication

unless a future requirement genuinely requires them.

The MVP is intentionally a standalone Android application.

---

## 21. Package Structure

A reasonable starting structure:

app/
└── src/main/java/.../
    ├── data/
    │   ├── local/
    │   │   ├── dao/
    │   │   ├── entity/
    │   │   └── database/
    │   └── repository/
    │
    ├── domain/
    │   ├── model/
    │   ├── parser/
    │   └── budget/
    │
    ├── notification/
    │   ├── NotificationListenerService
    │   └── NotificationProcessor
    │
    ├── widget/
    │
    ├── ui/
    │   ├── dashboard/
    │   ├── transactions/
    │   ├── budget/
    │   ├── settings/
    │   └── developer/
    │
    └── MainActivity

The exact package structure can change if the project remains small.

Do not over-engineer the architecture merely to follow a textbook.

---

## 22. Core Principle

Keep this separation:

Receiving notification
        ≠
Parsing notification
        ≠
Storing transaction
        ≠
Calculating spending
        ≠
Displaying spending

Each stage should have one clear responsibility.
