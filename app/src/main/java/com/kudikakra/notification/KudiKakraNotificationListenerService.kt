package com.kudikakra.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kudikakra.BuildConfig
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.domain.processor.TransactionProcessor
import com.kudikakra.notification.detection.DetectionResult
import com.kudikakra.notification.detection.FinancialNotificationDetector
import com.kudikakra.notification.detection.FinancialSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KudiKakraNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(
            AppDatabase.getInstance(applicationContext).userPreferencesDao()
        )
    }

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

        // Read user enabled sources from preferences and run detection.
        serviceScope.launch {
            val prefs = preferencesRepository.getPreferencesSync()
            val enabledSources = prefs?.getEnabledFinancialSources() ?: FinancialSource.entries.toSet()
            val detector = FinancialNotificationDetector(enabledSources = enabledSources)

            val detection = detector.detect(event)

            // Forward event and detection result to inspector store (memory-only, capped at 50).
            NotificationInspectorStore.add(event, detection)

            if (detection is DetectionResult.Financial && (prefs?.automaticTrackingEnabled == true)) {
                val transactionRepo = TransactionRepository(
                    AppDatabase.getInstance(applicationContext).transactionDao()
                )
                val processor = TransactionProcessor(transactionRepo, preferencesRepository)
                val result = processor.process(event)
                if (BuildConfig.DEBUG) Log.d(TAG, "Processed financial event: $result")
            }

            if (BuildConfig.DEBUG) {
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
        }

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "Notification received: package=${event.packageName}, " +
                    "title=${event.title}, text=${event.text}, " +
                    "timestamp=${event.timestampEpochMillis}"
            )
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
