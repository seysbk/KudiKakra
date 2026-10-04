package com.kudikakra

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kudikakra.notification.BudgetAlertWorker
import java.util.concurrent.TimeUnit

class KudiKakraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val workManager = WorkManager.getInstance(this)
        val immediateRequest = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
        workManager.enqueue(immediateRequest)

        val periodicRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            BudgetAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}
