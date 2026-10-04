package com.kudikakra.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.data.repository.DailyBudgetRepository
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.notification.NotificationInspectorStore
import com.kudikakra.notification.SpendingNotificationHelper
import com.kudikakra.notification.SpendingNotificationManager
import com.kudikakra.widget.SpendingWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = UserPreferencesRepository(db.userPreferencesDao())
    private val notificationHelper = SpendingNotificationHelper(
        TransactionRepository(db.transactionDao()),
        DailyBudgetRepository(db.dailyBudgetDao()),
        repository,
        SpendingNotificationManager(application),
    )

    val preferences = repository.observe()
        .map { it ?: UserPreferencesEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferencesEntity())

    fun setAutomaticTrackingEnabled(enabled: Boolean) = save {
        copy(automaticTrackingEnabled = enabled)
    }

    fun setSpendingNotificationsEnabled(enabled: Boolean) = save {
        copy(spendingNotificationsEnabled = enabled)
    }

    fun setNotificationThresholdPercent(percent: Int) = save {
        copy(notificationThresholdPercent = percent.coerceIn(1, 100))
    }

    fun setDeveloperModeEnabled(enabled: Boolean) = save {
        copy(developerModeEnabled = enabled)
    }

    fun toggleSource(source: com.kudikakra.notification.detection.FinancialSource, enabled: Boolean) = save {
        val currentSources = getEnabledFinancialSources().toMutableSet()
        if (enabled) {
            currentSources.add(source)
        } else {
            currentSources.remove(source)
        }
        val encoded = if (currentSources.isEmpty()) {
            UserPreferencesEntity.NONE_SELECTED
        } else {
            currentSources.joinToString(",") { it.name }
        }
        copy(selectedSources = encoded)
    }

    fun deleteAllLocalData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            db.transactionDao().deleteAll()
            db.dailyBudgetDao().deleteAll()
            repository.save(UserPreferencesEntity())
            NotificationInspectorStore.clear()
            SpendingNotificationManager.resetDeduplicationState()
            SpendingWidgetUpdater.update(getApplication())
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun save(update: UserPreferencesEntity.() -> UserPreferencesEntity) {
        viewModelScope.launch {
            repository.save(preferences.value.update().copy(updatedAtEpochMillis = System.currentTimeMillis()))
            notificationHelper.checkAndNotify()
        }
    }
}
