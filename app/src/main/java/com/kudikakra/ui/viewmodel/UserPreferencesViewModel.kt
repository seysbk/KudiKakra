package com.kudikakra.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(
        AppDatabase.getInstance(application).userPreferencesDao()
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

    private fun save(update: UserPreferencesEntity.() -> UserPreferencesEntity) {
        viewModelScope.launch {
            repository.save(preferences.value.update().copy(updatedAtEpochMillis = System.currentTimeMillis()))
        }
    }
}
