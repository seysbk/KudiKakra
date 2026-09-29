package com.kudikakra.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object SpendingWidgetUpdater {
    suspend fun update(context: Context) {
        runCatching {
            SpendingWidget().updateAll(context)
        }
    }
}
