package com.kudikakra

import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetEngineTest {
    @Test
    fun calculatesRemainingAndPercentage() {
        val summary = BudgetEngine.calculate(planMinorUnits = 6_000, spentMinorUnits = 4_800)

        assertEquals(1_200L, summary.remainingMinorUnits)
        assertEquals(80, summary.percentageUsed)
        assertEquals(BudgetStatus.APPROACHING_LIMIT, summary.status)
    }

    @Test
    fun marksPlanExceededWithoutNegativeRemaining() {
        val summary = BudgetEngine.calculate(planMinorUnits = 6_000, spentMinorUnits = 6_500)

        assertEquals(0L, summary.remainingMinorUnits)
        assertEquals(BudgetStatus.EXCEEDED, summary.status)
    }

    @Test
    fun handlesMissingPlan() {
        val summary = BudgetEngine.calculate(planMinorUnits = null, spentMinorUnits = 1_000)

        assertEquals(BudgetStatus.NO_PLAN, summary.status)
        assertEquals(null, summary.remainingMinorUnits)
    }
}
