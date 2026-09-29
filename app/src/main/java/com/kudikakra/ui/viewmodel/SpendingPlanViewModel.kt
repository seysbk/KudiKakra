package com.kudikakra.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.local.entity.DailyBudgetEntity
import com.kudikakra.data.repository.DailyBudgetRepository
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class SpendingPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val budgetRepository = DailyBudgetRepository(database.dailyBudgetDao())
    private val transactionRepository = TransactionRepository(database.transactionDao())

    val budgets = budgetRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val todaySpending = transactionRepository
        .observeExpenseTotal(todayStartMillis(), tomorrowStartMillis())

    val todaySummary = combine(budgets, todaySpending) { allBudgets, spent ->
        val todayPlan = allBudgets.firstOrNull { it.dayOfWeek == todayDayOfWeek() }
        BudgetEngine.calculate(todayPlan?.amountMinorUnits, spent)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BudgetEngine.calculate(null, 0L)
    )

    init {
        viewModelScope.launch {
            if (budgetRepository.observeAll().first().isEmpty()) {
                defaultPlans().forEach { budgetRepository.save(it) }
            }
        }
    }

    fun save(budget: DailyBudgetEntity) {
        viewModelScope.launch { budgetRepository.save(budget) }
    }

    private fun defaultPlans(): List<DailyBudgetEntity> {
        val amounts = mapOf(
            Calendar.MONDAY to 4_000L,
            Calendar.TUESDAY to 5_000L,
            Calendar.WEDNESDAY to 4_000L,
            Calendar.THURSDAY to 6_000L,
            Calendar.FRIDAY to 8_000L,
            Calendar.SATURDAY to 12_000L,
            Calendar.SUNDAY to 6_000L
        )
        val now = System.currentTimeMillis()
        return amounts.map { (day, amount) ->
            DailyBudgetEntity(dayOfWeek = day, amountMinorUnits = amount, updatedAtEpochMillis = now)
        }
    }
}

private fun todayDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

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
