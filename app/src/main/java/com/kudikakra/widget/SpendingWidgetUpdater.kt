package com.kudikakra.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object SpendingWidgetUpdater {
    suspend fun update(context: Context): Boolean =
        runCatching {
            SpendingWidget().updateAll(context)
            true
        }.getOrDefault(false)
}
