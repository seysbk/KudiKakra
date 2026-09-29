package com.kudikakra.domain.model

data class NormalizedTransaction(
    val amountMinorUnits: Long,
    val currency: String = "GHS",
    val type: TransactionType = TransactionType.UNKNOWN,
    val direction: TransactionDirection = TransactionDirection.UNKNOWN,
    val merchant: String? = null,
    val reference: String? = null,
    val source: String,
    val timestampEpochMillis: Long,
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW,
    val excludedFromSpending: Boolean = isDefaultExcluded(type, confidence)
) {
    companion object {
        fun isDefaultExcluded(type: TransactionType, confidence: ConfidenceLevel): Boolean {
            return when (type) {
                TransactionType.EXPENSE -> confidence != ConfidenceLevel.HIGH
                TransactionType.INCOME,
                TransactionType.TRANSFER,
                TransactionType.WITHDRAWAL,
                TransactionType.UNKNOWN -> true
            }
        }
    }
}
