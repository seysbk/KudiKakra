# KudiKakra

**How much do we have to spend today?**

KudiKakra is a privacy-focused Android spending-awareness application designed to make digital spending more visible and easier to understand.

The idea is simple: digital payments make it easy to spend from a larger balance without having a clear sense of how much has already been spent during the day. KudiKakra helps users set a daily spending plan and compare it with their actual spending.

The application is designed around local processing and minimal data collection. Its core functionality does not require a backend or cloud-based financial data storage.

## Current Status

KudiKakra is currently at the **prototype stage**.

The application has a functional Android interface and local data layer and can be installed and tested on a physical Android device.

### Implemented

- Jetpack Compose user interface
- Room database for local persistence
- Transaction data model and repository layer
- Transaction ViewModel
- Manual transaction entry and validation
- Transaction history with editing and deletion
- Weekday-based spending plans
- Local user preferences
- Daily spending calculations
- Remaining-spending and percentage calculations
- Approaching and exceeded budget states
- Dashboard
- Transaction history screen
- Daily spending-plan screen
- Settings screen
- Developer and testing screen
- Separation of expenses, income, transfers, withdrawals, and unknown transactions
- Initial notification-listening infrastructure for device testing

### Currently in Development

- Reliable financial notification detection
- Financial notification parsing
- Automatic transaction classification
- Provider-specific transaction parsers
- Confidence handling and review of uncertain transactions
- Automatic updates to daily spending
- Spending-awareness notifications
- Home-screen widget

The immediate technical milestone is to reliably process a supported financial transaction notification on a physical device, extract the relevant transaction information, and update the user's daily spending locally.

## How It Works

The intended transaction flow is:

```text
Financial Notification
        |
        v
Notification Listener
        |
        v
Source Detection
        |
        v
Financial Parser
        |
        v
Transaction Classification
        |
        v
Local Database
        |
        v
Daily Spending Calculation
        |
        +----> Dashboard
        |
        +----> Spending Notification
        |
        +----> Home-screen Widget
```

The application is designed to distinguish between different types of financial activity.

For example:

- Money spent is considered for daily expenditure.
- Money received is recorded separately and does not increase expenditure.
- Withdrawals are not automatically treated as spending because withdrawing cash does not necessarily mean the money has been spent.
- Transfers are not automatically treated as expenditure.
- Unknown or low-confidence transactions should not automatically increase spending totals.

This distinction is important because the application is intended to measure actual expenditure, rather than simply counting every transaction that moves money.

## Spending Plans

Users can define different spending plans for different days of the week.

For example:

| Day | Planned Spending |
| --- | --- |
| Monday | GH₵40 |
| Tuesday | GH₵50 |
| Wednesday | GH₵40 |
| Thursday | GH₵60 |
| Friday | GH₵80 |
| Saturday | GH₵120 |
| Sunday | GH₵60 |

KudiKakra compares the user's actual expenditure against the applicable daily plan.

The goal is not to prevent spending or control the user's money. The goal is to make spending visible enough for the user to make informed decisions.

## Privacy Approach

Privacy is a core design principle.

KudiKakra is being designed so that financial information can be processed locally on the user's device.

The intended privacy principles are:

- Core functionality should work without requiring an internet connection.
- Financial transaction data should remain on the device.
- Raw notification content should not be permanently stored unless explicitly required for development or debugging.
- Only the minimum information required for spending awareness should be retained.
- Unknown or low-confidence transactions should not automatically affect spending totals.
- The application does not move, hold, or control the user's money.
- The application should not require access to financial account credentials or transaction initiation.

## Technology

KudiKakra is being developed using:

- Kotlin
- Jetpack Compose
- Android SDK
- Gradle
- Room
- SQLite through Room
- Android NotificationListenerService
- Jetpack Glance for the planned home-screen widget

The initial architecture is intentionally local-first and does not require a dedicated backend.

## Development Architecture

The application is organized around a local processing pipeline:

```text
Android Notification
        |
        v
Notification Listener
        |
        v
Source Detector
        |
        v
Parser Registry
        |
        +---- Provider Parser
        |
        +---- Generic Parser
        |
        v
Normalized Transaction
        |
        v
Validation / Confidence
        |
        v
Transaction Repository
        |
        v
Room Database
        |
        v
Spending Engine
        |
        +---- Dashboard
        +---- Notifications
        +---- Widget
```

Provider-specific parsers are intended to be isolated from the rest of the application so that changes to one provider's notification format do not require rewriting the transaction system.

## MVP Direction

The initial MVP is intentionally focused.

The target experience is:

1. A user sets how much they plan to spend for the day.
2. KudiKakra detects supported financial transactions.
3. Relevant transactions are processed locally.
4. Actual expenditure is calculated.
5. The user can immediately see how much has been spent and how much remains from the day's plan.
6. KudiKakra can provide simple spending-awareness notifications and a home-screen widget.


The first major validation milestone is:

> A supported financial transaction notification is received on a real Android device, correctly interpreted, stored locally, and reflected in the user's daily spending.


## Development Roadmap

### Current

- Android project foundation
- Local database
- Manual transaction management
- Daily spending plans
- Dashboard and transaction history
- Notification-listening infrastructure
- Device testing

### Next

- Validate notification ingestion on physical devices
- Implement the first financial notification parser
- Implement automatic transaction classification
- Add confidence and review handling
- Connect detected transactions to daily spending calculations
- Add spending-awareness notifications
- Add home-screen widget

### Future

- Additional financial services and providers
- Improved transaction categorization
- Merchant recognition
- Spending patterns and personalized insights
- Unusual-spending detection
- Weekly spending reports
- Cash-expense tracking
- Open-finance and open-banking integrations where appropriate infrastructure becomes available
- Additional privacy-preserving features

Cloud synchronization, social features, advanced analytics, and AI-based processing are intentionally outside the initial MVP scope.

## Project Documentation

Additional project documentation is available in this repository:

- Architecture
- Parser Specification
- Project Context
- Development Roadmap

## Running the Project

1. Clone the repository.
2. Open the project root in Android Studio.
3. Allow Gradle to synchronize the project.
4. Connect a physical Android device with developer options and USB debugging enabled, or use an Android emulator.
5. Run the app configuration.


The current project supports Android SDK 24 and newer.

To verify Kotlin compilation from a Windows terminal:

```text
gradlew.bat :app:compileDebugKotlin
```

## Project Status

KudiKakra is an independently developed prototype exploring how digital financial activity can be made more visible without requiring users to manually record every transaction.

The project is being developed with an initial focus on everyday digital spending and is intended to evolve based on technical testing and user validation.

## Core Principle

Digital money made spending less visible.

KudiKakra is designed to make it visible again — without taking control of the user's money.