package com.kudikakra.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["fingerprint"], unique = true)]
)
data class TransactionEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,
    val amountMinorUnits: Long,
    val currency: String = "GHS",
    val type: TransactionType = TransactionType.UNKNOWN,
    val direction: TransactionDirection = TransactionDirection.UNKNOWN,
    val merchant: String? = null,
    val reference: String? = null,
    val timestampEpochMillis: Long,
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW,
    val category: String? = null,
    val excludedFromSpending: Boolean = true,
    val fingerprint: String? = null,
    val createdAtEpochMillis: Long
)
