package com.kudikakra.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kudikakra.BuildConfig
import com.kudikakra.notification.detection.DetectionResult
import com.kudikakra.notification.detection.FinancialNotificationDetector
import com.kudikakra.notification.detection.FinancialSource

class KudiKakraNotificationListenerService : NotificationListenerService() {

    /**
     * Detector instance.  Defaults to all known sources enabled.
     * In a future phase this will be configured from user preferences.
     */
    private val detector = FinancialNotificationDetector(
        enabledSources = FinancialSource.entries.toSet()
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationInspectorStore.setServiceConnected(true)
        if (BuildConfig.DEBUG) Log.d(TAG, "Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationInspectorStore.setServiceConnected(false)
        if (BuildConfig.DEBUG) Log.d(TAG, "Listener disconnected")
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Never process our own notifications.
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val event = NotificationEvent(
            packageName = sbn.packageName,
            title = extras.getCharSequence("android.title")?.toString().orEmpty(),
            text = notificationText(extras),
            timestampEpochMillis = sbn.postTime
        )

        // Always forward to the inspector store (memory-only, capped at 50).
        NotificationInspectorStore.add(event)

        // Run Phase 6 financial detection.
        val detection = detector.detect(event)

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "Notification received: package=${event.packageName}, " +
                    "title=${event.title}, text=${event.text}, " +
                    "timestamp=${event.timestampEpochMillis}"
            )
            when (detection) {
                is DetectionResult.Financial ->
                    Log.d(
                        TAG,
                        "Detection -> FINANCIAL " +
                            "confidence=${detection.confidence} " +
                            "source=${detection.matchedSource?.displayName ?: "unknown"} " +
                            "reason=${detection.reason}"
                    )
                is DetectionResult.NotFinancial ->
                    Log.d(TAG, "Detection -> NOT_FINANCIAL reason=${detection.reason}")
            }
        }

        // NOTE: Acting on financial detections (saving to Room, updating the
        // budget) is deliberately deferred to Phase 12 once the parser pipeline
        // (Phases 7–10) is in place.  For now, detection results are logged only.
    }

    private fun notificationText(extras: android.os.Bundle): String {
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (text.isNotBlank()) return text

        return extras.getCharSequenceArray("android.textLines")
            ?.joinToString(separator = "\n")
            .orEmpty()
    }

    private companion object {
        const val TAG = "KudiKakraNotifications"
    }
}
