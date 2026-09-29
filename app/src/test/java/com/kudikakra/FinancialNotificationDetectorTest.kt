package com.kudikakra

import com.kudikakra.notification.NotificationEvent
import com.kudikakra.notification.detection.DetectionResult
import com.kudikakra.notification.detection.FinancialConfidence
import com.kudikakra.notification.detection.FinancialNotificationDetector
import com.kudikakra.notification.detection.FinancialSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FinancialNotificationDetector].
 *
 * Tests are grouped by category:
 *
 * - True positives  — real financial notifications that must be detected.
 * - False positives — non-financial notifications that must NOT be detected.
 * - False negatives — financial notifications that must not be missed.
 * - Source filter   — respects the user's enabled-sources selection.
 * - Edge cases      — empty text, unusual formats.
 */
class FinancialNotificationDetectorTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Default detector: all sources enabled. */
    private val detector = FinancialNotificationDetector(
        enabledSources = FinancialSource.entries.toSet()
    )

    private fun event(
        packageName: String = "com.unknown.app",
        title: String = "",
        text: String,
    ) = NotificationEvent(
        packageName = packageName,
        title = title,
        text = text,
        timestampEpochMillis = System.currentTimeMillis(),
    )

    private fun mtnEvent(title: String = "MTN MoMo", text: String) =
        event(packageName = "com.mtn.momo", title = title, text = text)

    private fun gcbEvent(title: String = "GCB Bank", text: String) =
        event(packageName = "com.gcb.gcbmobile", title = title, text = text)

    // ── True positives — MTN MoMo ─────────────────────────────────────────────

    @Test
    fun `MTN MoMo expense notification is detected as Financial HIGH`() {
        val result = detector.detect(
            mtnEvent(text = "You have paid GH₵25.00 to Accra Groceries. Transaction ID: 12345")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    @Test
    fun `MTN MoMo received money notification is detected as Financial HIGH`() {
        val result = detector.detect(
            mtnEvent(text = "You have received GH₵500.00 from Kwame Mensah. Balance: GH₵750.00")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    @Test
    fun `MTN MoMo withdrawal notification is detected as Financial HIGH`() {
        val result = detector.detect(
            mtnEvent(text = "Cash withdrawal of GH₵800.00 successful. New balance: GH₵200.00")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    @Test
    fun `MTN MoMo transfer notification is detected as Financial HIGH`() {
        val result = detector.detect(
            mtnEvent(text = "You have sent GH₵200.00 to Ama Owusu. Transaction ID: 99887")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    @Test
    fun `MTN MoMo USSD-style notification with GHS currency is detected`() {
        val result = detector.detect(
            mtnEvent(text = "Payment of GHS 45.50 confirmed. Ref: MOMO12345")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    // ── True positives — GCB ─────────────────────────────────────────────────

    @Test
    fun `GCB Bank debit notification is detected as Financial HIGH`() {
        val result = detector.detect(
            gcbEvent(text = "Debit alert: GH₵150.00 debited from your account. Balance: GH₵3,200.00")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.GCB)
    }

    @Test
    fun `GCB Bank credit notification is detected as Financial HIGH`() {
        val result = detector.detect(
            gcbEvent(text = "Credit alert: GH₵2,000.00 credited to your account.")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.GCB)
    }

    // ── True positives — unknown package with strong keywords ─────────────────

    @Test
    fun `Unknown package with very strong financial keywords is detected MEDIUM`() {
        // Score: "ghs"(3) + "transaction"(3) + "debit"(3) + "amount"(2) = 11 >= 5
        val result = detector.detect(
            event(text = "GHS 50.00 debit transaction. Amount withdrawn from your account.")
        )
        assertFinancial(result, FinancialConfidence.MEDIUM, matchedSource = null)
    }

    // ── False positives — must NOT be detected as financial ───────────────────

    @Test
    fun `WhatsApp message mentioning paid is NOT financial`() {
        // "paid" alone (+2) is not enough for an unknown package (threshold = 3)
        val result = detector.detect(
            event(
                packageName = "com.whatsapp",
                title = "John",
                text = "I paid the bill, see you later!"
            )
        )
        assertNotFinancial(result)
    }

    @Test
    fun `WhatsApp message mentioning money is NOT financial`() {
        // "money" alone = +1, below threshold for unknown package
        val result = detector.detect(
            event(
                packageName = "com.whatsapp",
                title = "Group Chat",
                text = "Send money to the driver please"
            )
        )
        assertNotFinancial(result)
    }

    @Test
    fun `Generic SMS invitation is NOT financial`() {
        val result = detector.detect(
            event(
                packageName = "com.android.mms",
                text = "You have been invited to John's birthday party this Saturday!"
            )
        )
        assertNotFinancial(result)
    }

    @Test
    fun `Social media notification is NOT financial`() {
        val result = detector.detect(
            event(
                packageName = "com.instagram.android",
                title = "Instagram",
                text = "Kwame liked your photo."
            )
        )
        assertNotFinancial(result)
    }

    @Test
    fun `News app notification is NOT financial`() {
        val result = detector.detect(
            event(
                packageName = "com.bbc.news",
                title = "BBC News",
                text = "Ghana's economy grows as government releases new budget figures."
            )
        )
        // "budget" is not in our keyword list — should not trigger
        assertNotFinancial(result)
    }

    @Test
    fun `Email notification with purchase subject is NOT financial (score too low for unknown pkg)`() {
        // "payment" (+2) is present but score = 2, below unknown-package threshold of 3
        val result = detector.detect(
            event(
                packageName = "com.google.android.gm",
                title = "Your payment receipt",
                text = "Thank you for your order. Your payment has been processed."
            )
        )
        // "payment" appears twice (title + text) but keywords are matched once each
        // payment(2) + payment is already counted once = 2, below threshold 3
        // Actually "payment" appears in title AND text — our search text is "title text"
        // so it's present once in the combined string. Score = 2 < 3 -> NotFinancial
        assertNotFinancial(result)
    }

    // ── False negatives — financial notifications that must NOT be missed ──────

    @Test
    fun `MTN MoMo notification with no GHS symbol but strong keywords is not missed`() {
        // Known package, "debit"(3) + "transaction"(3) = score 6 >= HIGH threshold (3)
        val result = detector.detect(
            mtnEvent(text = "Debit transaction successful. Reference: TXN-88776")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    @Test
    fun `MTN MoMo notification with minimal text is detected at least LOW`() {
        // Known package, "momo"(3) alone = score 3 >= MEDIUM threshold (1) on known path
        val result = detector.detect(
            mtnEvent(title = "MoMo", text = "MoMo alert")
        )
        assertTrue(
            "Expected Financial result but got: $result",
            result is DetectionResult.Financial
        )
    }

    @Test
    fun `GCB notification with balance keyword is not missed`() {
        // Known package, "balance"(2) + "account"(1) = score 3 >= HIGH threshold
        val result = detector.detect(
            gcbEvent(text = "Your account balance has been updated.")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.GCB)
    }

    // ── Source filter — user selection ────────────────────────────────────────

    @Test
    fun `MTN MoMo notification ignored when MTN not in enabled sources`() {
        val gcbOnlyDetector = FinancialNotificationDetector(
            enabledSources = setOf(FinancialSource.GCB)
        )
        val result = gcbOnlyDetector.detect(
            mtnEvent(text = "You have paid GH₵50.00 to a merchant. Transaction ID: 555")
        )
        assertNotFinancial(result)
    }

    @Test
    fun `GCB notification ignored when GCB not in enabled sources`() {
        val mtnOnlyDetector = FinancialNotificationDetector(
            enabledSources = setOf(FinancialSource.MTN_MOMO)
        )
        val result = mtnOnlyDetector.detect(
            gcbEvent(text = "Debit alert: GH₵200.00 debited from your account.")
        )
        assertNotFinancial(result)
    }

    @Test
    fun `All sources disabled — known financial package is still ignored`() {
        val noSourcesDetector = FinancialNotificationDetector(enabledSources = emptySet())
        val result = noSourcesDetector.detect(
            mtnEvent(text = "You have paid GH₵100.00. Transaction ID: 999")
        )
        assertNotFinancial(result)
    }

    @Test
    fun `Unknown package with strong keywords still works when no specific source matches`() {
        // Unknown package is not filtered by source; only known packages are filtered.
        // Score: "gh₵"(3) + "transaction"(3) + "debit"(3) + "amount"(2) = 11 >= 5
        val gcbOnlyDetector = FinancialNotificationDetector(
            enabledSources = setOf(FinancialSource.GCB)
        )
        val result = gcbOnlyDetector.detect(
            event(
                packageName = "com.somebank.app",
                text = "GH₵ debit transaction amount confirmed from your account."
            )
        )
        // Unknown package + score >= 5 -> Financial MEDIUM
        assertFinancial(result, FinancialConfidence.MEDIUM, matchedSource = null)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `Empty text is NOT financial`() {
        val result = detector.detect(event(text = ""))
        assertNotFinancial(result)
    }

    @Test
    fun `Blank text is NOT financial`() {
        val result = detector.detect(event(text = "   "))
        assertNotFinancial(result)
    }

    @Test
    fun `Emoji-only notification is NOT financial`() {
        val result = detector.detect(event(text = "🎉🎂🥳"))
        assertNotFinancial(result)
    }

    @Test
    fun `Case-insensitive detection works for GHS`() {
        // "GHS" in uppercase should still score
        val result = detector.detect(
            mtnEvent(text = "GHS 30.00 debited. Transaction successful.")
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    @Test
    fun `MTN MoMo alternate package name is recognised`() {
        val result = detector.detect(
            event(
                packageName = "com.mtn.mtnmomopro",
                text = "You have paid GH₵60.00 for airtime."
            )
        )
        assertFinancial(result, FinancialConfidence.HIGH, FinancialSource.MTN_MOMO)
    }

    // ── Assertion helpers ─────────────────────────────────────────────────────

    private fun assertFinancial(
        result: DetectionResult,
        expectedConfidence: FinancialConfidence,
        matchedSource: FinancialSource?,
    ) {
        assertTrue("Expected Financial but got: $result", result is DetectionResult.Financial)
        val financial = result as DetectionResult.Financial
        assertEquals(
            "Confidence mismatch",
            expectedConfidence,
            financial.confidence
        )
        assertEquals(
            "MatchedSource mismatch",
            matchedSource,
            financial.matchedSource
        )
    }

    private fun assertNotFinancial(result: DetectionResult) {
        assertTrue("Expected NotFinancial but got: $result", result is DetectionResult.NotFinancial)
    }
}
