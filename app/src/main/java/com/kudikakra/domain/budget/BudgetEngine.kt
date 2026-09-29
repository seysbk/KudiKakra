package com.kudikakra.domain.budget

enum class BudgetStatus {
    NO_PLAN,
    ON_TRACK,
    APPROACHING_LIMIT,
    EXCEEDED
}

data class BudgetSummary(
    val planMinorUnits: Long?,
    val spentMinorUnits: Long,
    val remainingMinorUnits: Long?,
    val percentageUsed: Int,
    val status: BudgetStatus
)

object BudgetEngine {
    fun calculate(
        planMinorUnits: Long?,
        spentMinorUnits: Long,
        approachingThresholdPercent: Int = 80
    ): BudgetSummary {
        if (planMinorUnits == null || planMinorUnits <= 0) {
            return BudgetSummary(
                planMinorUnits = null,
                spentMinorUnits = spentMinorUnits,
                remainingMinorUnits = null,
                percentageUsed = 0,
                status = BudgetStatus.NO_PLAN
            )
        }

        val percentage = ((spentMinorUnits.toDouble() / planMinorUnits) * 100)
            .toInt()
            .coerceAtLeast(0)
        val status = when {
            spentMinorUnits > planMinorUnits -> BudgetStatus.EXCEEDED
            percentage >= approachingThresholdPercent -> BudgetStatus.APPROACHING_LIMIT
            else -> BudgetStatus.ON_TRACK
        }

        return BudgetSummary(
            planMinorUnits = planMinorUnits,
            spentMinorUnits = spentMinorUnits,
            remainingMinorUnits = (planMinorUnits - spentMinorUnits).coerceAtLeast(0L),
            percentageUsed = percentage,
            status = status
        )
    }
}
