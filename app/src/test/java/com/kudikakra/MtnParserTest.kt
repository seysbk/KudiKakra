package com.kudikakra

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.parser.MtnParser
import com.kudikakra.domain.parser.ParserRegistry
import com.kudikakra.notification.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MtnParserTest {

    private val parser = MtnParser()
    private val registry = ParserRegistry()

    // ── canHandle Tests ───────────────────────────────────────────────────────

    @Test
    fun `canHandle returns true for MTN MoMo package`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 20.00",
            timestampEpochMillis = 1000L
        )
        assertTrue(parser.canHandle(event))
    }

    @Test
    fun `canHandle returns true for SMS app with MoMo in text`() {
        val event = NotificationEvent(
            packageName = "com.google.android.apps.messaging",
            title = "MobileMoney",
            text = "Payment made for GH₵ 20.00",
            timestampEpochMillis = 1000L
        )
        assertTrue(parser.canHandle(event))
    }

    @Test
    fun `canHandle returns false for unrelated package and text`() {
        val event = NotificationEvent(
            packageName = "com.whatsapp",
            title = "John",
            text = "Hey, how are you?",
            timestampEpochMillis = 1000L
        )
        assertFalse(parser.canHandle(event))
    }

    // ── Expense Tests ─────────────────────────────────────────────────────────

    @Test
    fun `parse EXPENSE - payment made for GH₵ 25 to merchant`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo Alert",
            text = "Payment made for GH₵ 25.00 to Accra Groceries. Transaction ID: 10293847561. Fee charged: GH₵ 0.00. Balance: GH₵ 100.00.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(2500L, txn?.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertEquals("Accra Groceries", txn?.merchant)
        assertEquals("10293847561", txn?.reference)
        assertEquals(ConfidenceLevel.HIGH, txn?.confidence)
        assertFalse(txn?.excludedFromSpending ?: true)
    }

    @Test
    fun `parse EXPENSE - paid GH₵ 50 to merchant with reference`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have paid GH₵ 50.00 to Supermarket. Reference: Groceries. Txn ID: 998877.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(5000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertEquals("Supermarket", txn?.merchant)
        assertEquals("998877", txn?.reference)
        assertFalse(txn?.excludedFromSpending ?: true)
    }

    @Test
    fun `parse EXPENSE - airtime purchase`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment of GH₵ 10.00 made to MTN Airtime. Transaction ID: 987654.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(1000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertEquals("MTN Airtime", txn?.merchant)
        assertFalse(txn?.excludedFromSpending ?: true)
    }

    // ── Income Tests ──────────────────────────────────────────────────────────

    @Test
    fun `parse INCOME - amount received from sender`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "An amount of GH₵ 200.00 has been received from JOHN DOE. Transaction ID: 12345. Current Balance: GH₵ 500.00.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(20000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.INCOME, txn?.type)
        assertEquals(TransactionDirection.IN, txn?.direction)
        assertEquals("JOHN DOE", txn?.merchant)
        assertEquals("12345", txn?.reference)
        assertTrue(txn?.excludedFromSpending ?: false)
    }

    @Test
    fun `parse INCOME - you have received GH₵ 150`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have received GH₵ 150.00 from KWAME ASANTE.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(15000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.INCOME, txn?.type)
        assertEquals(TransactionDirection.IN, txn?.direction)
        assertEquals("KWAME ASANTE", txn?.merchant)
        assertTrue(txn?.excludedFromSpending ?: false)
    }

    // ── Transfer Tests ────────────────────────────────────────────────────────

    @Test
    fun `parse TRANSFER - transferred to recipient`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have transferred GH₵ 100.00 to KOFI MENSAH. Transaction ID: 88776655. Fee charged: GH₵ 0.75.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(10000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.TRANSFER, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertEquals("KOFI MENSAH", txn?.merchant)
        assertEquals("88776655", txn?.reference)
        assertTrue(txn?.excludedFromSpending ?: false)
    }

    @Test
    fun `parse TRANSFER - sent to number`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "An amount of GH₵ 50.00 sent to 0244123456. Transaction ID: 334455.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(5000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.TRANSFER, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertEquals("0244123456", txn?.merchant)
        assertTrue(txn?.excludedFromSpending ?: false)
    }

    // ── Withdrawal Tests ──────────────────────────────────────────────────────

    @Test
    fun `parse WITHDRAWAL - Cash Out from agent`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Cash Out of GH₵ 300.00 from Agent AGENT NAME. Transaction ID: 55443322. Fee charged: GH₵ 3.00.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(30000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.WITHDRAWAL, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertEquals("AGENT NAME", txn?.merchant)
        assertEquals("55443322", txn?.reference)
        assertTrue(txn?.excludedFromSpending ?: false)
    }

    @Test
    fun `parse WITHDRAWAL - withdrawn GH₵ 200`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have withdrawn GH₵ 200.00 from Agent 12345.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(20000L, txn?.amountMinorUnits)
        assertEquals(TransactionType.WITHDRAWAL, txn?.type)
        assertEquals(TransactionDirection.OUT, txn?.direction)
        assertTrue(txn?.excludedFromSpending ?: false)
    }

    // ── Unknown / Non-Financial Tests ─────────────────────────────────────────

    @Test
    fun `parse UNKNOWN - PIN change notification`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Your MoMo PIN was changed successfully.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertFalse(result.success)
        assertNull(result.transaction?.merchant)
        assertEquals(ConfidenceLevel.LOW, result.confidence)
    }

    @Test
    fun `parse UNKNOWN - failed transaction`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Transaction failed due to insufficient balance.",
            timestampEpochMillis = 1000L
        )

        val result = parser.parse(event)
        assertFalse(result.success)
    }

    // ── Currency Format & Registry Integration ────────────────────────────────

    @Test
    fun `parse with GHS and large amount with comma`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Transferred GHS 1,234.50 to Ecobank Ghana Account 1234. Reference: school fees.",
            timestampEpochMillis = 1000L
        )

        val result = registry.parse(event)
        assertTrue(result.success)
        val txn = result.transaction
        assertNotNull(txn)
        assertEquals(123450L, txn?.amountMinorUnits)
        assertEquals(TransactionType.TRANSFER, txn?.type)
        assertTrue(txn?.excludedFromSpending ?: false)
    }
}
