package com.kudikakra.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(
        AppDatabase.getInstance(application).transactionDao()
    )

    val transactions = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayExpenseMinorUnits = repository
        .observeExpenseTotal(todayStartMillis(), tomorrowStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun insert(transaction: TransactionEntity) {
        viewModelScope.launch { repository.insert(transaction) }
    }

    fun update(transaction: TransactionEntity) {
        viewModelScope.launch { repository.update(transaction) }
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch { repository.delete(transaction) }
    }
}

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
