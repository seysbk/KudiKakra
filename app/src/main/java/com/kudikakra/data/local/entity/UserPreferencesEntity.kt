package com.kudikakra.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.notification.detection.FinancialSource

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val automaticTrackingEnabled: Boolean = false,
    val spendingNotificationsEnabled: Boolean = true,
    val notificationThresholdPercent: Int = 80,
    val selectedSources: String = "",
    val merchantRules: String = "",
    val developerModeEnabled: Boolean = false,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun getEnabledFinancialSources(): Set<FinancialSource> {
        if (selectedSources.isBlank()) return FinancialSource.entries.toSet()
        if (selectedSources == NONE_SELECTED) return emptySet()
        val names = selectedSources.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        return FinancialSource.entries.filter { it.name in names }.toSet()
    }

    fun getMerchantRules(): Map<String, TransactionType> {
        if (merchantRules.isBlank()) return emptyMap()
        return merchantRules.split(";")
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val type = runCatching { TransactionType.valueOf(parts[1].trim().uppercase()) }.getOrNull()
                    if (key.isNotBlank() && type != null) key to type else null
                } else null
            }
            .toMap()
    }

    fun withMerchantRule(merchant: String, type: TransactionType): UserPreferencesEntity {
        val cleanKey = merchant.trim().lowercase()
        if (cleanKey.isBlank()) return this
        val currentRules = getMerchantRules().toMutableMap()
        currentRules[cleanKey] = type
        val serialized = currentRules.entries.joinToString(";") { "${it.key}=${it.value.name}" }
        return copy(merchantRules = serialized, updatedAtEpochMillis = System.currentTimeMillis())
    }

    companion object {
        const val NONE_SELECTED = "NONE"
    }
}
