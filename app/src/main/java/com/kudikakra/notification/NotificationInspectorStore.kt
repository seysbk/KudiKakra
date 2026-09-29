package com.kudikakra.notification

import com.kudikakra.notification.detection.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InspectedNotification(
    val event: NotificationEvent,
    val detectionResult: DetectionResult? = null
)

object NotificationInspectorStore {
    private const val MAX_EVENTS = 50

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _inspectedNotifications = MutableStateFlow<List<InspectedNotification>>(emptyList())
    val inspectedNotifications: StateFlow<List<InspectedNotification>> = _inspectedNotifications.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun add(event: NotificationEvent, detectionResult: DetectionResult? = null) {
        val item = InspectedNotification(event, detectionResult)
        _inspectedNotifications.value = (listOf(item) + _inspectedNotifications.value).take(MAX_EVENTS)
    }

    fun clear() {
        _inspectedNotifications.value = emptyList()
    }
}
