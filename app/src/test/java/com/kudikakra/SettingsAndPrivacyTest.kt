package com.kudikakra

import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetStatus
import com.kudikakra.notification.NotificationEvent
import com.kudikakra.notification.NotificationInspectorStore
import com.kudikakra.notification.detection.DetectionResult
import com.kudikakra.notification.detection.FinancialConfidence
import com.kudikakra.notification.detection.FinancialNotificationDetector
import com.kudikakra.notification.detection.FinancialSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsAndPrivacyTest {

    @Before
    fun setUp() {
        NotificationInspectorStore.clear()
    }

    @Test
    fun `UserPreferencesEntity defaults prioritize user privacy`() {
        val prefs = UserPreferencesEntity()
        assertFalse("Automatic tracking should be opt-in (disabled by default)", prefs.automaticTrackingEnabled)
        assertTrue("Spending notifications enabled by default", prefs.spendingNotificationsEnabled)
        assertEquals(80, prefs.notificationThresholdPercent)
        assertFalse("Developer mode disabled by default", prefs.developerModeEnabled)
    }

    @Test
    fun `UserPreferencesEntity handles notification threshold bounding`() {
        val validPrefs = UserPreferencesEntity(notificationThresholdPercent = 75)
        assertEquals(75, validPrefs.notificationThresholdPercent)
    }

    @Test
    fun `UserPreferencesEntity toggling developer mode works correctly`() {
        val prefs = UserPreferencesEntity()
        val devModePrefs = prefs.copy(developerModeEnabled = true)
        assertTrue(devModePrefs.developerModeEnabled)
    }

    @Test
    fun `FinancialNotificationDetector respects selected financial sources`() {
        val mtnEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MobileMoney",
            text = "Payment made for GH₵ 50.00 to Supermarket. Transaction ID: 123456.",
            timestampEpochMillis = 1000L
        )

        // Case 1: MTN MoMo is enabled -> Detection should succeed as Financial
        val detectorWithMtn = FinancialNotificationDetector(enabledSources = setOf(FinancialSource.MTN_MOMO))
        val result1 = detectorWithMtn.detect(mtnEvent)
        assertTrue(result1 is DetectionResult.Financial)
        assertEquals(FinancialSource.MTN_MOMO, (result1 as DetectionResult.Financial).matchedSource)

        // Case 2: MTN MoMo is disabled (only GCB enabled) -> Detection should return NotFinancial
        val detectorWithoutMtn = FinancialNotificationDetector(enabledSources = setOf(FinancialSource.GCB))
        val result2 = detectorWithoutMtn.detect(mtnEvent)
        assertTrue(result2 is DetectionResult.NotFinancial)
    }

    @Test
    fun `NotificationInspectorStore caps transient events at 50 for privacy and memory management`() {
        for (i in 1..60) {
            val event = NotificationEvent(
                packageName = "com.test.app",
                title = "Title $i",
                text = "Sample text $i",
                timestampEpochMillis = i.toLong()
            )
            NotificationInspectorStore.add(event, null)
        }

        val items = NotificationInspectorStore.inspectedNotifications.value
        assertEquals(50, items.size)
        // Ensure oldest items were evicted and newest items remain
        assertEquals("Title 60", items.first().event.title)
        assertEquals("Title 11", items.last().event.title)
    }

    @Test
    fun `BudgetEngine calculates approaching threshold status based on configured percentage`() {
        val plan = 100_00L // GH₵100.00

        // At 75% spending with threshold = 80%, status should be ON_TRACK
        val summaryBelowThreshold = BudgetEngine.calculate(
            planMinorUnits = plan,
            spentMinorUnits = 75_00L,
            approachingThresholdPercent = 80
        )
        assertEquals(BudgetStatus.ON_TRACK, summaryBelowThreshold.status)

        // At 85% spending with threshold = 80%, status should be APPROACHING_LIMIT
        val summaryAboveThreshold = BudgetEngine.calculate(
            planMinorUnits = plan,
            spentMinorUnits = 85_00L,
            approachingThresholdPercent = 80
        )
        assertEquals(BudgetStatus.APPROACHING_LIMIT, summaryAboveThreshold.status)

        // With lower threshold = 70%, 75% spending triggers APPROACHING_LIMIT
        val summaryWithLowerThreshold = BudgetEngine.calculate(
            planMinorUnits = plan,
            spentMinorUnits = 75_00L,
            approachingThresholdPercent = 70
        )
        assertEquals(BudgetStatus.APPROACHING_LIMIT, summaryWithLowerThreshold.status)
    }
}
