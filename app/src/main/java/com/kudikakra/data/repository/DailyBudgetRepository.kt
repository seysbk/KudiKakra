package com.kudikakra.data.repository

import com.kudikakra.data.local.dao.DailyBudgetDao
import com.kudikakra.data.local.entity.DailyBudgetEntity
import kotlinx.coroutines.flow.Flow

class DailyBudgetRepository(private val dao: DailyBudgetDao) {
    fun observeAll(): Flow<List<DailyBudgetEntity>> = dao.observeAll()

    suspend fun getByDay(dayOfWeek: Int): DailyBudgetEntity? = dao.getByDay(dayOfWeek)

    suspend fun save(budget: DailyBudgetEntity) = dao.upsert(budget)
}
