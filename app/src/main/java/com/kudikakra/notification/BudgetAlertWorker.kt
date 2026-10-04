package com.kudikakra.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.repository.DailyBudgetRepository
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.widget.SpendingWidgetUpdater

class BudgetAlertWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = runCatching {
        val database = AppDatabase.getInstance(applicationContext)
        val transactionRepository = TransactionRepository(database.transactionDao())
        val preferencesRepository = UserPreferencesRepository(database.userPreferencesDao())
        val budgetRepository = DailyBudgetRepository(database.dailyBudgetDao())

        SpendingNotificationHelper(
            transactionRepository,
            budgetRepository,
            preferencesRepository,
            SpendingNotificationManager(applicationContext),
        ).checkAndNotify()

        if (SpendingWidgetUpdater.update(applicationContext)) Result.success() else Result.retry()
    }.getOrElse { Result.retry() }

    companion object {
        const val WORK_NAME = "daily_budget_alert_check"
    }
}