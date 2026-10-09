package com.kudikakra.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kudikakra.R
import com.kudikakra.MainActivity
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetSummary
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

class SpendingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(120.dp, 84.dp), DpSize(180.dp, 116.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = getTodayBudgetSummary(context)
        val refreshedAt = System.currentTimeMillis()

        provideContent {
            GlanceTheme {
                SpendingWidgetContent(summary = summary, refreshedAt = refreshedAt)
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
    refreshedAt: Long = System.currentTimeMillis(),
    modifier: GlanceModifier = GlanceModifier
) {
    val spentFormatted = formatMoney(summary.spentMinorUnits)
    val planFormatted = summary.planMinorUnits?.let { formatMoney(it) } ?: "No plan"
    val remainingFormatted = summary.remainingMinorUnits?.let { "${formatMoney(it)} remaining" } ?: "No plan set"
    val progress = summary.percentageUsed.coerceIn(0, 100)
    val dayLabel = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(refreshedAt))
    val refreshLabel = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(refreshedAt))

    Column(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(14.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today · $dayLabel",
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier
                    .width(18.dp)
                    .height(18.dp)
                    .clickable(actionRunCallback<RefreshSpendingWidgetAction>())
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = "$spentFormatted / $planFormatted",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp)
                .cornerRadius(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalSegments = 20
            val activeSegments = (progress * totalSegments / 100).coerceIn(0, totalSegments)
            for (i in 1..totalSegments) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(8.dp)
                        .background(
                            if (i <= activeSegments) GlanceTheme.colors.primary
                            else GlanceTheme.colors.surfaceVariant
                        )
                ) {}
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = "$remainingFormatted · ${progress.coerceIn(0, 100)}%",
            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
        )
        Text(
            text = "Updated $refreshLabel",
            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
        )
    }
}

class RefreshSpendingWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        SpendingWidget().update(context, glanceId)
    }
}

private fun formatMoney(amountMinorUnits: Long): String =
    "GH₵${String.format(Locale.US, "%,.2f", amountMinorUnits / 100.0)}"
