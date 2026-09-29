package com.kudikakra.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    WITHDRAWAL,
    UNKNOWN
}

enum class TransactionDirection {
    IN,
    OUT,
    UNKNOWN
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}
