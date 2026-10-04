# KudiKakra — Project Context

## 1. Project Overview

KudiKakra is a privacy-focused Android spending-awareness application.

MVP focus:

The first product milestone is intentionally small: a user should be able to manually record spending, set a daily plan, and reliably see how much they have spent today. Automatic notification parsing should be added only after this flow works reliably.

The MVP should begin with one conservative provider parser. Multiple providers, widgets, spending notifications, advanced analytics, AI, cloud backup, and synchronization are deferred until the core manual flow is proven.

The core problem is that physical cash naturally makes spending visible. Before going out, a person may take a "sufficient" amount of money for the day. At the end of the day, they can look at what remains and roughly determine how much they spent.

Digital money changes this.

With MoMo, bank accounts and other digital financial platforms, a person spends from their total available money rather than from a consciously separated amount intended for that particular day. Transaction histories exist, but checking them can feel like:

«"checking how much water you fetched from a river with a cup."»

Digital payments are convenient, fast and almost effortless. Those are useful qualities, but they can also make it easy to lose good money without noticing how quickly it is happening.

KudiKakra aims to make digital spending visible again.

---

## 2. Core Product Idea

KudiKakra automatically detects supported financial transaction notifications on the user's Android device.

The application:

1. Receives financial notifications from services the user chooses to track.
2. Determines whether a notification represents a financial transaction.
3. Determines whether the transaction is money spent/sent or money received.
4. Extracts relevant information locally.
5. Stores the normalized transaction data locally.
6. Counts genuine expenditure toward the user's spending plan.
7. Compares actual expenditure against the user's planned spending.
8. Shows the current spending state through the app.
9. Provides optional spending notifications.
10. Provides a home-screen widget showing essential spending information.

The MVP is local-first.

There is no backend, cloud database, financial API, remote AI service or account system required for the core product.

---

## 3. Primary Goal

The main question KudiKakra should help answer is:

«"I planned to spend X today. How much have I actually spent?"»

The product is primarily about money spent or lost, not about displaying a user's total financial wealth or account balance.

The home screen should therefore focus on expenditure and the user's spending plan.

Example:

```text
«Today's Spending
GH₵48 / GH₵60
GH₵12 remaining»
```

---

## 4. What Counts as Spending?

The application must distinguish between:

### EXPENSE

Money genuinely spent on something.

Examples:

- buying food
- paying for transport
- paying a merchant
- purchasing goods
- paying a bill
- paying for a service

These transactions contribute to the user's daily expenditure.

### TRANSFER / SENT

Money sent from the user's account to another person or account.

For MVP purposes, a sent transaction may be treated as expenditure only when the system has sufficient evidence that it represents actual spending.

If it is clearly a transfer between the user's own accounts or a movement of money that should not count as spending, it should be excluded from expenditure.

### INCOME / RECEIVED

Money received by the user.

Examples:

- money received from another person
- salary
- deposit
- refund
- transfer into the account

Received money is not expenditure.

The MVP may store received transactions for classification/history purposes, but the primary dashboard and spending calculations should not count ordinary income as money spent.

---

## 5. Large Withdrawals / Cash-Outs

A major edge case is a user receiving or withdrawing a large amount of money that they have not actually spent.

Example:

```text
A user receives GH₵1,000 and later withdraws GH₵800 in cash.
```

The GH₵800 withdrawal does not necessarily mean the user spent GH₵800.

It may simply mean the user moved money from digital form into physical cash.

Therefore:

```text
«A withdrawal/cash-out must NOT automatically be treated as expenditure.»
```

The transaction should be classified as:

```text
"WITHDRAWAL"
```

and excluded from spending calculations by default.

The user may later manually mark part of the cash as spent if they want to track it.

This distinction is important because the purpose of KudiKakra is to measure actual spending rather than simply measure money leaving a particular digital account.

---

## 6. Privacy Philosophy

Financial information is highly sensitive.

The MVP should follow a strict local-first approach.

Financial notification contents should be processed on the device whenever possible.

The application should store only the structured information needed for its functionality.

Avoid permanently storing complete raw notification text unless it is explicitly required for a development/debugging feature.

The MVP should not:

- upload financial notifications to a server
- send financial data to an AI provider
- require a user to provide financial account credentials
- access a user's MoMo account directly
- initiate financial transactions
- control the user's money
- display account balances as a primary feature
- require a cloud account for basic functionality

---

## 7. Supported Sources

The user should select which financial services they want KudiKakra to monitor.

Examples may eventually include:

- MTN MoMo
- GCB
- GhanaPay
- Telecel Cash
- AirtelTigo Money
- other supported banks/payment services

The MVP should begin with a very small number of real sources.

Do not implement every Ghanaian financial service at once.

The architecture must make adding a new parser easy later.

---

## 8. How Transactions Enter the System

The MVP is notification-first.

A transaction may have been initiated through:

- a financial application
- USSD
- another supported method

KudiKakra is interested in the resulting transaction notification received on the device, not primarily in how the transaction was initiated.

However, notification behavior must be tested on real devices because not every service or transaction method is guaranteed to produce an observable notification in the same way.

---

## 9. Spending Plans

Users can configure a different spending plan for each day of the week.

Example:

- Monday — GH₵40
- Tuesday — GH₵50
- Wednesday — GH₵40
- Thursday — GH₵60
- Friday — GH₵80
- Saturday — GH₵120
- Sunday — GH₵60

KudiKakra compares:

Planned spending vs Actual expenditure

It should not prevent spending.

It should provide information at the right time.

---

## 10. User Experience

The application should feel informative rather than judgmental.

Good:

«You planned GH₵60 today.
You've spent GH₵48.
GH₵12 remaining.»

Good:

«You've spent GH₵52 of your GH₵60 plan today.»

Avoid language such as:

«You wasted money.»

«You are spending irresponsibly.»

The application provides information. The user makes their own decisions.

---

## 11. Home-Screen Widget

The widget is a core MVP feature.

The widget should show essential information without requiring the user to open the application.

Initial design:

«TODAY
GH₵48 / GH₵60
GH₵12 remaining»

Do not over-engineer the widget initially.

A simple useful widget is better than a complex widget with charts and unnecessary information.

---

## 12. Notifications

KudiKakra may notify users when:

- they are approaching their spending plan
- they have exceeded their spending plan
- another useful spending-awareness condition occurs

Example:

«You've spent GH₵52 of your GH₵60 plan.
GH₵8 remaining.»

The notification system should be configurable and not become intrusive.

---

## 13. MVP Boundaries

The MVP should focus on:

1. Android application
2. Local Room database
3. NotificationListenerService
4. Financial notification detection
5. Provider-specific parsers
6. Expense/income/transfer/withdrawal classification
7. Daily spending plans
8. Spending calculation
9. Spending-awareness notifications
10. Home-screen widget
11. Local privacy

The MVP does NOT require:

- Django
- PostgreSQL
- Firebase
- cloud synchronization
- user accounts
- online financial APIs
- Open Banking
- AI
- receipt scanning
- voice input
- advanced analytics
- social features
- payment functionality

---

## 14. Long-Term Direction

Future versions may include:

- more financial institutions
- better transaction categorisation
- merchant recognition
- unusual spending detection
- spending velocity
- weekly reports
- personalised insights
- recurring spending detection
- better handling of cash withdrawals
- optional manual cash-expense entry
- Open Banking integrations when appropriate infrastructure becomes available
- optional AI-assisted analysis

These are future features and must not unnecessarily complicate the MVP.

---

## 15. Product Philosophy

KudiKakra is not a bank.

It is not a MoMo replacement.

It is not a payment application.

It does not control money.

It is a spending-awareness layer that sits above the user's existing financial services.

The fundamental idea is:

«Digital money made spending invisible.
KudiKakra makes it visible again.»

"What Are We Spending Today?"
