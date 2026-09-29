package com.kudikakra.data.local.entity

import androidx.room.Entity

@Entity(tableName = "daily_budgets")
data class DailyBudgetEntity(
    @androidx.room.PrimaryKey
    val dayOfWeek: Int,
    val amountMinorUnits: Long,
    val currency: String = "GHS",
    val updatedAtEpochMillis: Long
)
