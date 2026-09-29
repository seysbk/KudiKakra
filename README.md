# KudiKakra

KudiKakra is a privacy-focused Android spending-awareness app for making digital spending easier to see and understand.

The app is designed to process supported financial notifications locally, classify transactions, and compare actual spending with a daily spending plan.

## Current status

The project is in the early MVP stage.

Implemented so far:

- Basic Jetpack Compose UI.
- Room database foundation for local persistence.
- Transaction and daily-budget repositories and ViewModels.
- Manual transaction entry with validation.
- Transaction history with editing and deletion.
- Room-backed user preferences and weekday spending plans.
- Daily budget summaries with remaining, percentage, approaching, and exceeded states.
- Notification listener service and in-memory notification inspector for device testing.
- Dashboard screen.
- Transactions screen.
- Daily spending-plan screen.
- Settings screen.
- Developer/testing screen.
- Simple navigation between screens.
- Clear separation between expenses, income, transfers, withdrawals, and unknown transactions in the UI.

Not implemented yet:

- Notification listener service.
- Financial notification parsers.
- Automatic transaction classification.
- Spending notifications.
- Home-screen widget.

## MVP direction

The first success milestone is intentionally small:

> A user can manually record spending, set a daily plan, and reliably see how much they have spent today.

The planned implementation order is:

1. Basic UI — complete.
2. Room database and repositories.
3. Manual transactions.
4. Daily spending calculations and plans.
5. Notification listener testing.
6. One conservative provider parser.
7. Review and correction for uncertain transactions.
8. Widget and spending-awareness notifications.

Multiple providers, cloud services, AI, synchronization, and advanced analytics are intentionally outside the initial MVP.

## Technology

- Kotlin
- Jetpack Compose
- Android SDK
- Gradle
- Room, planned for local persistence
- NotificationListenerService, planned for transaction detection
- Jetpack Glance, planned for the home-screen widget

## Running the project

1. Open the project root in Android Studio:

   ```text
   C:\Users\Seyram\Desktop\KudiKakra
   ```

2. Allow Gradle to sync the project.
3. Connect an Android device or start an emulator.
4. Run the `app` configuration.

The project currently requires Android SDK 24 or newer.

From a terminal, the debug Kotlin compilation can be checked with:

```text
gradlew.bat :app:compileDebugKotlin
```

## Privacy principles

- Core functionality should work without an internet connection.
- Financial data should remain on the device.
- Raw notification text should not be stored permanently unless explicitly needed for development.
- Unknown or low-confidence transactions must not automatically increase spending.
- Withdrawals and transfers are not automatically treated as expenses.

## Project documentation

- [Architecture](Architecture.md)
- [Parser Specification](Parser%20Specification.md)
- [Project Context](Project%20Context.md)
- [TODO](TODO.md)
