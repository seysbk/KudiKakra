package com.kudikakra

import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.notification.NotificationEvent
import com.kudikakra.notification.NotificationInspectorStore
import com.kudikakra.notification.detection.DetectionResult
import com.kudikakra.notification.detection.FinancialConfidence
import com.kudikakra.notification.detection.FinancialSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationInspectorAndPreferencesTest {

    @Before
    fun setUp() {
        NotificationInspectorStore.clear()
        NotificationInspectorStore.setServiceConnected(false)
    }

    @Test
    fun `NotificationInspectorStore manages service connection state`() {
        assertFalse(NotificationInspectorStore.isServiceConnected.value)
        NotificationInspectorStore.setServiceConnected(true)
        assertTrue(NotificationInspectorStore.isServiceConnected.value)
    }

    @Test
    fun `NotificationInspectorStore stores and clears inspected notifications`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment confirmed",
            timestampEpochMillis = 1000L
        )
        val detection = DetectionResult.Financial(
            confidence = FinancialConfidence.HIGH,
            matchedSource = FinancialSource.MTN_MOMO,
            reason = "Test"
        )

        NotificationInspectorStore.add(event, detection)
        assertEquals(1, NotificationInspectorStore.inspectedNotifications.value.size)

        val item = NotificationInspectorStore.inspectedNotifications.value.first()
        assertEquals(event, item.event)
        assertEquals(detection, item.detectionResult)

        NotificationInspectorStore.clear()
        assertTrue(NotificationInspectorStore.inspectedNotifications.value.isEmpty())
    }

    @Test
    fun `UserPreferencesEntity parses enabled sources correctly`() {
        // Default (blank) should enable all sources
        val defaultPrefs = UserPreferencesEntity()
        assertEquals(FinancialSource.entries.toSet(), defaultPrefs.getEnabledFinancialSources())

        // Specific sources saved
        val mtnOnlyPrefs = UserPreferencesEntity(selectedSources = "MTN_MOMO")
        assertEquals(setOf(FinancialSource.MTN_MOMO), mtnOnlyPrefs.getEnabledFinancialSources())

        // Multiple sources
        val multiPrefs = UserPreferencesEntity(selectedSources = "MTN_MOMO,GCB")
        assertEquals(setOf(FinancialSource.MTN_MOMO, FinancialSource.GCB), multiPrefs.getEnabledFinancialSources())

        // None selected
        val nonePrefs = UserPreferencesEntity(selectedSources = UserPreferencesEntity.NONE_SELECTED)
        assertTrue(nonePrefs.getEnabledFinancialSources().isEmpty())
    }
}
