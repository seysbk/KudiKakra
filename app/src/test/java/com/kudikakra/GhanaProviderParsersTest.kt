package com.kudikakra

import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.parser.GcbParser
import com.kudikakra.domain.parser.ParserRegistry
import com.kudikakra.domain.parser.TelecelCashParser
import com.kudikakra.notification.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GhanaProviderParsersTest {
    private val gcbParser = GcbParser()
    private val telecelParser = TelecelCashParser()

    @Test
    fun `GCB SMS debit alert is parsed as an expense`() {
        val event = NotificationEvent(
            packageName = "com.google.android.apps.messaging",
            title = "GCB Bank",
            text = "Debit alert: GH₵125.00 debited from your account for POS purchase at Melcom. Ref: GCB7788",
            timestampEpochMillis = 1000L,
        )

        val result = gcbParser.parse(event)
        val transaction = result.transaction

        assertTrue(result.success)
        assertEquals(12500L, transaction?.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, transaction?.type)
        assertEquals(TransactionDirection.OUT, transaction?.direction)
        assertEquals("GCB7788", transaction?.reference)
    }

    @Test
    fun `GCB debit alert keeps transaction amount and merchant separate from fee and account`() {
        val result = gcbParser.parse(
            event(
                "com.google.android.apps.messaging",
                "GCB Bank",
                "Fee: GH₵2.00. Debit alert: GH₵125.00 debited from your account for POS purchase at Melcom. Ref: GCB7789",
            )
        )

        assertEquals(12500L, result.transaction?.amountMinorUnits)
        assertEquals("Melcom", result.transaction?.merchant)
    }

    @Test
    fun `Telecel Cash fee does not become transaction amount`() {
        val result = telecelParser.parse(
            event(
                "com.telecelcash",
                "Telecel Cash",
                "Fee GH₵1.00. You paid GH₵25.00 to Kwame. Ref: TC125",
            )
        )

        assertEquals(2500L, result.transaction?.amountMinorUnits)
        assertEquals("Kwame", result.transaction?.merchant)
    }

    @Test
    fun `GCB credit alert is income rather than expenditure`() {
        val event = NotificationEvent(
            packageName = "com.gcb.gcbmobile",
            title = "GCB Bank",
            text = "Credit alert: GHS 500.00 credited to your account. Ref: 881122",
            timestampEpochMillis = 1000L,
        )

        val transaction = gcbParser.parse(event).transaction

        assertEquals(TransactionType.INCOME, transaction?.type)
        assertEquals(TransactionDirection.IN, transaction?.direction)
        assertTrue(transaction?.excludedFromSpending == true)
    }

    @Test
    fun `Telecel Cash transfer and cash out keep non-expense semantics`() {
        val transfer = telecelParser.parse(
            event("com.telecelcash", "Telecel Cash", "You have transferred GH₵30.00 to Ama. Transaction ID: TC123")
        ).transaction
        val withdrawal = telecelParser.parse(
            event("com.telecel.cash", "Telecel Cash", "Cash out GH₵40.00 from agent. Ref: TC124")
        ).transaction

        assertEquals(TransactionType.TRANSFER, transfer?.type)
        assertEquals(TransactionDirection.OUT, transfer?.direction)
        assertEquals(TransactionType.WITHDRAWAL, withdrawal?.type)
        assertEquals(TransactionDirection.OUT, withdrawal?.direction)
    }

    @Test
    fun `provider brands in SMS sender are routed and non-transaction notices ignored`() {
        val smsEvent = event(
            "com.google.android.apps.messaging",
            "Telecel Cash",
            "You received GH₵70.00 from Kofi. Ref: TC998",
        )
        val unrelated = event("com.telecelcash", "Telecel Cash", "Your profile details were updated successfully.")

        assertTrue(telecelParser.canHandle(smsEvent))
        assertEquals(TransactionType.INCOME, ParserRegistry().parse(smsEvent).transaction?.type)
        assertFalse(gcbParser.parse(unrelated).success)
        assertNull(gcbParser.parse(unrelated).transaction)
    }

    private fun event(packageName: String, title: String, text: String) = NotificationEvent(
        packageName = packageName,
        title = title,
        text = text,
        timestampEpochMillis = 1000L,
    )
}