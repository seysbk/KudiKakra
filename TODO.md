KudiKakra — MVP TODO

Status Legend

- [ ] Not started
- [~] In progress
- [x] Completed
- [!] Blocked / needs investigation

MVP SCOPE GUARDRAILS

The first success milestone is deliberately small:

> A user can manually record spending, set a daily plan, and reliably see how much they have spent today.

Do not expand the MVP until this flow works reliably. Postpone multiple providers, the home-screen widget, spending notifications, parser versioning, advanced analytics, AI, cloud backup, and synchronization.

Recommended implementation order:

1. Basic dashboard
2. Manual transactions
3. Room storage
4. Today's spending calculation
5. Daily spending plans
6. Expense/income/transfer/withdrawal tests
7. Notification listener experiment
8. One conservative provider parser
9. Review and correction for uncertain transactions
10. Widget and spending notifications

Automatic classification rules:

- HIGH-confidence expenses may be saved automatically.
- MEDIUM-confidence transactions should require confirmation.
- LOW-confidence or UNKNOWN transactions must not increase spending.
- Income, transfers, and withdrawals must remain separate from expenses.

---

PHASE 0 — Project Setup

- [ ] Create Android Studio project
- [ ] Use Kotlin
- [ ] Use Jetpack Compose
- [ ] Confirm project builds successfully
- [ ] Connect physical Android phone
- [ ] Run basic app on physical phone
- [ ] Confirm Logcat works
- [ ] Open same project in VS Code
- [ ] Confirm AI coding agent can read and modify the project
- [ ] Add Project Context.md
- [ ] Add Architecture.md
- [ ] Add Parser Specification.md
- [ ] Add TODO.md

---

PHASE 1 — Basic UI

- [x] Create basic navigation
- [x] Create Dashboard screen
- [x] Create Transactions screen
- [x] Create Spending Plan screen
- [x] Create Settings screen
- [x] Create Developer/Testing screen
- [x] Use simple Compose UI
- [x] Do not over-design the UI yet

---

PHASE 2 — Room / SQLite

- [x] Add Room
- [x] Create AppDatabase
- [x] Create Transaction entity
- [x] Create DailyBudget entity
- [x] Create UserPreferences entity
- [x] Create TransactionDao
- [x] Create DailyBudgetDao
- [x] Create repositories
- [x] Create ViewModels
- [~] Verify database writes
- [~] Verify database reads
- [ ] Verify app survives restart with data intact

---

PHASE 3 — Manual Transactions

Build manual entry before automatic detection.

- [x] Add manual expense entry
- [x] Add amount
- [x] Add merchant
- [x] Add source
- [x] Add date/time
- [x] Add transaction type
- [x] Save to Room
- [x] Display transaction history
- [x] Allow deleting a transaction
- [x] Allow editing a transaction
- [~] Test database thoroughly

This gives the project a working foundation before notification parsing is introduced.

---

PHASE 4 — Spending Plan

- [x] Allow user to configure Monday budget
- [x] Allow Tuesday budget
- [x] Allow Wednesday budget
- [x] Allow Thursday budget
- [x] Allow Friday budget
- [x] Allow Saturday budget
- [x] Allow Sunday budget
- [x] Save plans to Room
- [x] Display today's plan
- [x] Calculate today's expenditure
- [x] Calculate remaining amount
- [x] Calculate percentage used
- [x] Detect approaching limit
- [x] Detect exceeded limit

Example:

Plan: GH₵60
Spent: GH₵48
Remaining: GH₵12

---

PHASE 5 — Notification Listener

Make-or-break technical experiment

- [x] Implement NotificationListenerService
- [x] Request/guide user through notification access
- [x] Verify service starts
- [x] Verify service receives notifications
- [x] Log package name
- [x] Log notification title
- [x] Log notification text
- [x] Log timestamp
- [x] Create Notification Inspector screen
- [x] Do NOT store all raw notifications permanently

Real-device testing

Test:

- [ ] MTN MoMo app transaction
- [ ] MTN MoMo USSD transaction
- [ ] GCB transaction
- [ ] Other supported service
- [ ] Normal WhatsApp notification
- [ ] Normal SMS notification
- [ ] Other unrelated notification

Record what the phone actually exposes.

---

PHASE 6 — Financial Detector

- [x] Create FinancialNotificationDetector
- [x] Identify likely financial notifications
- [x] Ignore unrelated notifications
- [x] Identify selected financial sources
- [x] Return confidence
- [x] Test false positives
- [x] Test false negatives

The detector should be conservative.

---

PHASE 7 — Parser Framework

- [x] Create NotificationEvent
- [x] Create FinancialParser interface
- [x] Create ParseResult
- [x] Create ParserRegistry
- [x] Create normalized Transaction model
- [x] Add transaction direction
- [x] Add transaction type
- [x] Add confidence
- [x] Add excludedFromSpending
- [x] Add parser unit tests

---

PHASE 8 — First Provider Parser

Start with ONE provider.

Recommended starting point:

MTNParser

- [ ] Collect representative notification examples
- [ ] Identify expense messages
- [ ] Identify received messages
- [ ] Identify sent/transfer messages
- [ ] Identify withdrawal messages
- [ ] Identify amount formats
- [ ] Identify merchant/reference formats
- [ ] Implement parser
- [ ] Add unit tests
- [ ] Test parser with copied notification examples
- [ ] Test parser against real device notifications

Do not add five providers before the first parser works reliably.

---

PHASE 9 — Transaction Classification

- [ ] Correctly identify EXPENSE
- [ ] Correctly identify INCOME
- [ ] Correctly identify TRANSFER
- [ ] Correctly identify WITHDRAWAL
- [ ] Correctly identify UNKNOWN
- [ ] Verify income is excluded from expenditure
- [ ] Verify withdrawals are excluded from expenditure
- [ ] Verify transfers are not automatically treated as expenses
- [ ] Verify genuine expenses increase expenditure

Important test:

Receive GH₵1,000
Withdraw GH₵800
Spend GH₵50

Expected expenditure:
GH₵50

Not:

GH₵850

---

PHASE 10 — Confidence + Review

- [ ] HIGH confidence auto-save
- [ ] MEDIUM confidence review/confirmation
- [ ] LOW confidence ignore or hold
- [ ] Create review UI if needed
- [ ] Allow user to correct classification
- [ ] Learn from corrections only if a safe deterministic mechanism exists

Do not add AI yet.

---

PHASE 11 — Deduplication

- [ ] Identify duplicate notification scenarios
- [ ] Create transaction fingerprint
- [ ] Prevent duplicate spending records
- [ ] Test repeated notifications
- [ ] Test delayed notifications
- [ ] Test app restart during processing

---

PHASE 12 — Automatic Spending Updates

When a valid expense is detected:

Notification
 ↓
Parser
 ↓
Transaction
 ↓
Room
 ↓
Budget Engine
 ↓
Dashboard

- [ ] Automatically save high-confidence expenses
- [ ] Recalculate today's expenditure
- [ ] Update remaining amount
- [ ] Update dashboard immediately
- [ ] Confirm income does not affect expenditure
- [ ] Confirm withdrawals do not affect expenditure

---

PHASE 13 — Spending Notifications

- [ ] Create notification channel
- [ ] Create approaching-budget notification
- [ ] Create exceeded-budget notification
- [ ] Avoid duplicate notifications
- [ ] Add user-configurable threshold
- [ ] Allow notifications to be disabled
- [ ] Test notification behavior on real phone

Example:

You've spent GH₵52 of your GH₵60 plan.
GH₵8 remaining.

---

PHASE 14 — Home-Screen Widget

Use Jetpack Glance.

- [ ] Create basic widget
- [ ] Display today's plan
- [ ] Display today's expenditure
- [ ] Display remaining amount
- [ ] Update widget after transaction
- [ ] Update widget after budget change
- [ ] Handle new day
- [ ] Test widget after phone restart

Initial widget:

TODAY

GH₵48 / GH₵60

GH₵12 remaining

Do not add charts yet.

---

PHASE 15 — Settings

- [ ] Select financial sources
- [ ] Enable/disable automatic tracking
- [ ] Configure notification threshold
- [ ] Enable/disable spending notifications
- [ ] Provide notification-access status
- [ ] Provide privacy explanation
- [ ] Add data deletion option
- [ ] Add developer/test mode only where appropriate

---

PHASE 16 — Privacy

- [ ] Confirm no financial data leaves device
- [ ] Confirm no backend exists
- [ ] Confirm no cloud database exists
- [ ] Confirm no AI API is used
- [ ] Minimize raw notification storage
- [ ] Review Android permissions
- [ ] Review exported components
- [ ] Review logs for accidental financial data exposure
- [ ] Remove sensitive debug logging from release build
- [ ] Test app without internet

---

PHASE 17 — Real-Device Testing

Test on the actual target phone.

Transaction tests

- [ ] Expense
- [ ] Income
- [ ] Transfer
- [ ] Withdrawal
- [ ] Failed transaction
- [ ] Reversed transaction if observable
- [ ] Duplicate notification
- [ ] Delayed notification
- [ ] Large withdrawal
- [ ] Small expense
- [ ] Multiple expenses in one day

Budget tests

- [ ] Spend below plan
- [ ] Spend exactly plan
- [ ] Exceed plan
- [ ] No transactions
- [ ] New day
- [ ] Different plan for each weekday
- [ ] Change plan
- [ ] Delete transaction
- [ ] Correct transaction

Device tests

- [ ] App restarted
- [ ] Phone restarted
- [ ] Notification listener restarted
- [ ] Battery optimization behavior checked
- [ ] App backgrounded
- [ ] Phone offline
- [ ] Widget refreshed
- [ ] Notification permissions/access revoked

---

PHASE 18 — Second Provider

Only after the first provider is reliable.

- [ ] Add GCBParser
- [ ] Add tests
- [ ] Test income
- [ ] Test expense
- [ ] Test transfer
- [ ] Test withdrawal
- [ ] Test duplicates
- [ ] Confirm normalized output is unchanged

Then consider additional providers.

---

PHASE 19 — MVP Polish

- [ ] Improve dashboard
- [ ] Improve spending-plan setup
- [ ] Improve transaction history
- [ ] Improve widget appearance
- [ ] Improve empty states
- [ ] Add loading states where needed
- [ ] Add useful error messages
- [ ] Remove unnecessary developer UI
- [ ] Remove debug logs
- [ ] Test clean installation
- [ ] Test upgrade/update behavior

---

PHASE 20 — MVP Completion Criteria

The MVP is considered "finished" when a real user can:

1. Install KudiKakra.
2. Select a supported financial service.
3. Give the required Android notification access.
4. Set different spending plans for different days.
5. Make a real supported transaction.
6. Have KudiKakra detect the transaction.
7. Correctly determine whether it was money spent, received, transferred or withdrawn.
8. Store the normalized transaction locally.
9. See today's actual expenditure.
10. See the remaining amount against today's plan.
11. Receive a spending-awareness notification when appropriate.
12. See the same information on the home-screen widget.
13. Use the core functionality without an internet connection.
14. Delete or correct incorrectly recorded transactions.

---

FUTURE — Do Not Build During Initial MVP

These are deliberately postponed:

- [ ] More financial providers
- [ ] SMS ingestion
- [ ] Open Banking
- [ ] AI transaction categorisation
- [ ] AI spending insights
- [ ] Unusual-spending detection
- [ ] Spending velocity analysis
- [ ] Weekly reports
- [ ] Recurring expense detection
- [ ] Advanced charts
- [ ] Merchant learning
- [ ] Cash-expense tracking
- [ ] Multi-device synchronization
- [ ] Cloud backup
- [ ] User accounts
- [ ] Social features
- [ ] Receipt scanning
- [ ] Voice expense entry
- [ ] Payment functionality

The MVP should prove the central idea before any of these are added.
