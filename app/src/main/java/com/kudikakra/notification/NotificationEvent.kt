package com.kudikakra.notification

data class NotificationEvent(
    val packageName: String,
    val title: String,
    val text: String,
    val timestampEpochMillis: Long
)
