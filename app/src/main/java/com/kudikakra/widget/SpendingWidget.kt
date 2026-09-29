package com.kudikakra.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kudikakra.MainActivity
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetSummary
import java.util.Calendar
import java.util.Locale

class SpendingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = getTodayBudgetSummary(context)

        provideContent {
            GlanceTheme {
                SpendingWidgetContent(summary = summary)
            }
        }
    }

    suspend fun getTodayBudgetSummary(context: Context): BudgetSummary {
        val db = AppDatabase.getInstance(context)
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayBudget = db.dailyBudgetDao().getByDay(todayDay)

        val spent = db.transactionDao().getExpenseTotal(
            startMillis = todayStartMillis(),
            endMillis = tomorrowStartMillis()
        )

        return BudgetEngine.calculate(
            planMinorUnits = todayBudget?.amountMinorUnits,
            spentMinorUnits = spent
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

@Composable
fun SpendingWidgetContent(
    summary: BudgetSummary,
    modifier: GlanceModifier = GlanceModifier
) {
    val spentFormatted = formatMoney(summary.spentMinorUnits)
    val planFormatted = summary.planMinorUnits?.let { formatMoney(it) } ?: "No plan"
    val remainingFormatted = summary.remainingMinorUnits?.let { "${formatMoney(it)} remaining" } ?: "No plan set"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TODAY",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = "$spentFormatted / $planFormatted",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = remainingFormatted,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

private fun formatMoney(amountMinorUnits: Long): String =
    "GH₵${String.format(Locale.US, "%,.2f", amountMinorUnits / 100.0)}"
