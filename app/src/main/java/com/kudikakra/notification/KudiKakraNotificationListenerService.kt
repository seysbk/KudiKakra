package com.kudikakra.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kudikakra.BuildConfig

class KudiKakraNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val event = NotificationEvent(
            packageName = sbn.packageName,
            title = extras.getCharSequence("android.title")?.toString().orEmpty(),
            text = notificationText(extras),
            timestampEpochMillis = sbn.postTime
        )

        NotificationInspectorStore.add(event)

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "Notification received: package=${event.packageName}, " +
                    "title=${event.title}, text=${event.text}, " +
                    "timestamp=${event.timestampEpochMillis}"
            )
        }
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
