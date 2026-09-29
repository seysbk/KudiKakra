package com.kudikakra.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kudikakra.data.local.entity.DailyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyBudgetDao {
    @Query("SELECT * FROM daily_budgets ORDER BY dayOfWeek")
    fun observeAll(): Flow<List<DailyBudgetEntity>>

    @Query("SELECT * FROM daily_budgets WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun getByDay(dayOfWeek: Int): DailyBudgetEntity?

    @Upsert
    suspend fun upsert(budget: DailyBudgetEntity)

    @Query("DELETE FROM daily_budgets")
    suspend fun deleteAll()
}
