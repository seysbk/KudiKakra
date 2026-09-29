package com.kudikakra.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kudikakra.notification.detection.FinancialSource

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val automaticTrackingEnabled: Boolean = false,
    val spendingNotificationsEnabled: Boolean = true,
    val notificationThresholdPercent: Int = 80,
    val selectedSources: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun getEnabledFinancialSources(): Set<FinancialSource> {
        if (selectedSources.isBlank()) return FinancialSource.entries.toSet()
        if (selectedSources == NONE_SELECTED) return emptySet()
        val names = selectedSources.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        return FinancialSource.entries.filter { it.name in names }.toSet()
    }

    companion object {
        const val NONE_SELECTED = "NONE"
    }
}
