package com.kudikakra.notification

import com.kudikakra.data.repository.DailyBudgetRepository
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.domain.budget.BudgetEngine
import java.util.Calendar

class SpendingNotificationHelper(
    private val transactionRepository: TransactionRepository,
    private val dailyBudgetRepository: DailyBudgetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationManager: SpendingNotificationManager
) {
    suspend fun checkAndNotify(): Boolean {
        val prefs = userPreferencesRepository.getPreferencesSync()
        val enabled = prefs?.spendingNotificationsEnabled ?: true
        if (!enabled) return false

        val threshold = prefs?.notificationThresholdPercent ?: 80
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayBudget = dailyBudgetRepository.getByDay(todayDay)

        val spent = transactionRepository.getExpenseTotal(
            startMillis = todayStartMillis(),
            endMillis = tomorrowStartMillis()
        )

        val summary = BudgetEngine.calculate(
            planMinorUnits = todayBudget?.amountMinorUnits,
            spentMinorUnits = spent,
            approachingThresholdPercent = threshold
        )

        return notificationManager.evaluateAndNotify(
            summary = summary,
            spendingNotificationsEnabled = enabled
        )
    }

    companion object {
        private fun todayStartMillis(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        private fun tomorrowStartMillis(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}
