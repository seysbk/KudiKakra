package com.kudikakra.data.local

import androidx.room.TypeConverter
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType

class KudiKakraConverters {
    @TypeConverter
    fun transactionTypeToString(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun stringToTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun directionToString(value: TransactionDirection?): String? = value?.name

    @TypeConverter
    fun stringToDirection(value: String?): TransactionDirection? =
        value?.let { runCatching { TransactionDirection.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun confidenceToString(value: ConfidenceLevel?): String? = value?.name

    @TypeConverter
    fun stringToConfidence(value: String?): ConfidenceLevel? =
        value?.let { runCatching { ConfidenceLevel.valueOf(it) }.getOrNull() }
}
