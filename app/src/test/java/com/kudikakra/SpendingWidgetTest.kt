package com.kudikakra

import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SpendingWidgetTest {

    @Test
    fun testBudgetSummaryCalculationForWidget_withPlanAndExpenses() {
        // Spent GH₵48 (4800 minor units), Plan GH₵60 (6000 minor units)
        val summary = BudgetEngine.calculate(
            planMinorUnits = 6000L,
            spentMinorUnits = 4800L
        )

        assertEquals(6000L, summary.planMinorUnits)
        assertEquals(4800L, summary.spentMinorUnits)
        assertEquals(1200L, summary.remainingMinorUnits)
        assertEquals(80, summary.percentageUsed)
        assertEquals(BudgetStatus.APPROACHING_LIMIT, summary.status)

        val spentFormatted = formatMoney(summary.spentMinorUnits)
        val planFormatted = summary.planMinorUnits?.let { formatMoney(it) } ?: "No plan"
        val remainingFormatted = summary.remainingMinorUnits?.let { "${formatMoney(it)} remaining" } ?: "No plan set"

        assertEquals("GH₵48.00", spentFormatted)
        assertEquals("GH₵60.00", planFormatted)
        assertEquals("GH₵12.00 remaining", remainingFormatted)
    }

    @Test
    fun testBudgetSummaryCalculationForWidget_noPlanSet() {
        val summary = BudgetEngine.calculate(
            planMinorUnits = null,
            spentMinorUnits = 2500L
        )

        assertEquals(null, summary.planMinorUnits)
        assertEquals(2500L, summary.spentMinorUnits)
        assertEquals(null, summary.remainingMinorUnits)
        assertEquals(BudgetStatus.NO_PLAN, summary.status)

        val spentFormatted = formatMoney(summary.spentMinorUnits)
        val planFormatted = summary.planMinorUnits?.let { formatMoney(it) } ?: "No plan"
        val remainingFormatted = summary.remainingMinorUnits?.let { "${formatMoney(it)} remaining" } ?: "No plan set"

        assertEquals("GH₵25.00", spentFormatted)
        assertEquals("No plan", planFormatted)
        assertEquals("No plan set", remainingFormatted)
    }

    @Test
    fun testBudgetSummaryCalculationForWidget_exceededPlan() {
        // Spent GH₵80 (8000 minor units), Plan GH₵60 (6000 minor units)
        val summary = BudgetEngine.calculate(
            planMinorUnits = 6000L,
            spentMinorUnits = 8000L
        )

        assertEquals(0L, summary.remainingMinorUnits)
        assertEquals(133, summary.percentageUsed)
        assertEquals(BudgetStatus.EXCEEDED, summary.status)

        val spentFormatted = formatMoney(summary.spentMinorUnits)
        val remainingFormatted = summary.remainingMinorUnits?.let { "${formatMoney(it)} remaining" } ?: "No plan set"

        assertEquals("GH₵80.00", spentFormatted)
        assertEquals("GH₵0.00 remaining", remainingFormatted)
    }

    private fun formatMoney(amountMinorUnits: Long): String =
        "GH₵${String.format(Locale.US, "%,.2f", amountMinorUnits / 100.0)}"
}
