package com.kudikakra.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationInspectorStore {
    private const val MAX_EVENTS = 50

    private val _events = MutableStateFlow<List<NotificationEvent>>(emptyList())
    val events: StateFlow<List<NotificationEvent>> = _events.asStateFlow()

    fun add(event: NotificationEvent) {
        _events.value = (listOf(event) + _events.value).take(MAX_EVENTS)
    }

    fun clear() {
        _events.value = emptyList()
    }
}
