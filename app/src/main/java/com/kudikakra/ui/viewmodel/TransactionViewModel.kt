package com.kudikakra.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = TransactionRepository(db.transactionDao())
    private val userPreferencesRepository = UserPreferencesRepository(db.userPreferencesDao())

    val transactions = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingReviewTransactions = repository.observePendingReview()
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

    fun confirmTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val confirmed = transaction.copy(
                confidence = ConfidenceLevel.HIGH,
                excludedFromSpending = (transaction.type != TransactionType.EXPENSE)
            )
            repository.update(confirmed)

            transaction.merchant?.let { merchant ->
                if (merchant.isNotBlank()) {
                    val prefs = userPreferencesRepository.getPreferencesSync() ?: UserPreferencesEntity()
                    userPreferencesRepository.save(prefs.withMerchantRule(merchant, transaction.type))
                }
            }
        }
    }

    fun correctTransaction(transaction: TransactionEntity, newType: TransactionType, newMerchant: String?) {
        viewModelScope.launch {
            val merchantToUse = newMerchant?.ifBlank { null } ?: transaction.merchant
            val updated = transaction.copy(
                type = newType,
                merchant = merchantToUse,
                confidence = ConfidenceLevel.HIGH,
                excludedFromSpending = (newType != TransactionType.EXPENSE)
            )
            repository.update(updated)

            merchantToUse?.let { merchant ->
                if (merchant.isNotBlank()) {
                    val prefs = userPreferencesRepository.getPreferencesSync() ?: UserPreferencesEntity()
                    userPreferencesRepository.save(prefs.withMerchantRule(merchant, newType))
                }
            }
        }
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
