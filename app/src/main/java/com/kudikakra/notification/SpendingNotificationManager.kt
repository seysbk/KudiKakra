package com.kudikakra.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kudikakra.R
import com.kudikakra.domain.budget.BudgetStatus
import com.kudikakra.domain.budget.BudgetSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class SpendingNotificationManager(private val context: Context? = null) {

    private val notificationManager: NotificationManager? by lazy {
        context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = context ?: return
            val nm = notificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Checks budget status and posts notification if status is APPROACHING_LIMIT or EXCEEDED.
     * Prevents duplicate notifications for the same status on the same day.
     *
     * @return true if a notification was posted, false otherwise.
     */
    fun evaluateAndNotify(
        summary: BudgetSummary,
        spendingNotificationsEnabled: Boolean
    ): Boolean {
        if (!spendingNotificationsEnabled) return false
        if (summary.planMinorUnits == null || summary.planMinorUnits <= 0) return false

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val statusKey = summary.status.name

        val storedState = context?.getSharedPreferences(DEDUP_PREFERENCES, Context.MODE_PRIVATE)
        val previousDate = storedState?.getString(KEY_LAST_DATE, lastNotifiedDate) ?: lastNotifiedDate
        val previousStatus = storedState?.getString(KEY_LAST_STATUS, lastNotifiedStatus) ?: lastNotifiedStatus
        if (previousDate == todayDate && previousStatus == statusKey) {
            return false
        }

        return when (summary.status) {
            BudgetStatus.APPROACHING_LIMIT -> {
                val title = "Approaching Daily Budget"
                val text = buildApproachingMessage(summary)
                if (!postNotification(NOTIFICATION_ID_APPROACHING, title, text)) return false
                recordNotificationState(todayDate, statusKey)
                true
            }
            BudgetStatus.EXCEEDED -> {
                val title = "Daily Budget Exceeded"
                val text = buildExceededMessage(summary)
                if (!postNotification(NOTIFICATION_ID_EXCEEDED, title, text)) return false
                recordNotificationState(todayDate, statusKey)
                true
            }
            else -> false
        }
    }

    private fun recordNotificationState(date: String, status: String) {
        lastNotifiedDate = date
        lastNotifiedStatus = status
        context?.getSharedPreferences(DEDUP_PREFERENCES, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_LAST_DATE, date)
            ?.putString(KEY_LAST_STATUS, status)
            ?.apply()
    }

    protected open fun postNotification(notificationId: Int, title: String, text: String): Boolean {
        val ctx = context ?: return false
        val nm = notificationManager ?: return false
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        return runCatching {
            nm.notify(notificationId, builder.build())
            true
        }.getOrDefault(false)
    }

    private fun buildApproachingMessage(summary: BudgetSummary): String {
        val spentStr = formatMoney(summary.spentMinorUnits)
        val planStr = formatMoney(summary.planMinorUnits ?: 0L)
        val remainingStr = formatMoney(summary.remainingMinorUnits ?: 0L)
        return "You've spent $spentStr of your $planStr plan. $remainingStr remaining."
    }

    private fun buildExceededMessage(summary: BudgetSummary): String {
        val spentStr = formatMoney(summary.spentMinorUnits)
        val planStr = formatMoney(summary.planMinorUnits ?: 0L)
        val excessMinor = summary.spentMinorUnits - (summary.planMinorUnits ?: 0L)
        val excessStr = formatMoney(excessMinor)
        return "You've spent $spentStr today, exceeding your $planStr plan by $excessStr."
    }

    private fun formatMoney(amountMinorUnits: Long): String =
        "GH₵${String.format(Locale.US, "%,.2f", amountMinorUnits / 100.0)}"

    companion object {
        const val CHANNEL_ID = "spending_alerts"
        const val CHANNEL_NAME = "Spending Alerts"
        const val CHANNEL_DESCRIPTION = "Alerts when approaching or exceeding daily spending limit"

        const val NOTIFICATION_ID_APPROACHING = 1001
        const val NOTIFICATION_ID_EXCEEDED = 1002
        private const val DEDUP_PREFERENCES = "spending_notification_state"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_LAST_STATUS = "last_status"

        // Memory-cached last notification date and status for duplicate prevention
        var lastNotifiedDate: String? = null
        var lastNotifiedStatus: String? = null

        fun resetDeduplicationState() {
            lastNotifiedDate = null
            lastNotifiedStatus = null
        }
    }
}
