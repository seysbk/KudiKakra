package com.kudikakra

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.NormalizedTransaction
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.parser.MtnParser
import com.kudikakra.notification.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionClassificationTest {

    private val parser = MtnParser()

    @Test
    fun `EXPENSE classification sets excludedFromSpending to false`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 50.00 to Supermarket.",
            timestampEpochMillis = System.currentTimeMillis()
        )
        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction!!
        assertEquals(TransactionType.EXPENSE, txn.type)
        assertEquals(TransactionDirection.OUT, txn.direction)
        assertFalse("Genuine expense must NOT be excluded from spending", txn.excludedFromSpending)
    }

    @Test
    fun `INCOME classification sets excludedFromSpending to true`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "An amount of GH₵ 1,000.00 has been received from JOHN DOE.",
            timestampEpochMillis = System.currentTimeMillis()
        )
        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction!!
        assertEquals(TransactionType.INCOME, txn.type)
        assertEquals(TransactionDirection.IN, txn.direction)
        assertTrue("Income must be excluded from spending", txn.excludedFromSpending)
    }

    @Test
    fun `TRANSFER classification sets excludedFromSpending to true`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have transferred GH₵ 500.00 to KOFI MENSAH.",
            timestampEpochMillis = System.currentTimeMillis()
        )
        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction!!
        assertEquals(TransactionType.TRANSFER, txn.type)
        assertEquals(TransactionDirection.OUT, txn.direction)
        assertTrue("Transfer must be excluded from spending", txn.excludedFromSpending)
    }

    @Test
    fun `WITHDRAWAL classification sets excludedFromSpending to true`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Cash Out of GH₵ 800.00 from Agent AGENT NAME.",
            timestampEpochMillis = System.currentTimeMillis()
        )
        val result = parser.parse(event)
        assertTrue(result.success)
        val txn = result.transaction!!
        assertEquals(TransactionType.WITHDRAWAL, txn.type)
        assertEquals(TransactionDirection.OUT, txn.direction)
        assertTrue("Withdrawal must be excluded from spending", txn.excludedFromSpending)
    }

    @Test
    fun `UNKNOWN classification sets excludedFromSpending to true`() {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Your MoMo PIN was changed successfully.",
            timestampEpochMillis = System.currentTimeMillis()
        )
        val result = parser.parse(event)
        assertFalse(result.success)
        val txn = result.transaction
        if (txn != null) {
            assertEquals(TransactionType.UNKNOWN, txn.type)
            assertTrue("Unknown transactions must be excluded from spending", txn.excludedFromSpending)
        }
    }

    @Test
    fun `Phase 9 Benchmark Test - Receive GH1,000, Withdraw GH800, Spend GH50 produces GH50 total expenditure`() {
        // Scenario from Phase 9 specification:
        // 1. Receive GH₵1,000
        val incomeEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "An amount of GH₵ 1,000.00 has been received from Employer Inc.",
            timestampEpochMillis = 1000L
        )

        // 2. Withdraw GH₵800
        val withdrawalEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Cash Out of GH₵ 800.00 from Agent 001.",
            timestampEpochMillis = 2000L
        )

        // 3. Spend GH₵50
        val expenseEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 50.00 to Local Diner.",
            timestampEpochMillis = 3000L
        )

        val transactions = listOf(
            parser.parse(incomeEvent).transaction!!,
            parser.parse(withdrawalEvent).transaction!!,
            parser.parse(expenseEvent).transaction!!
        )

        // Calculate expenditure according to KudiKakra spending rules
        val totalExpenditureMinorUnits = transactions
            .filter { !it.excludedFromSpending && it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinorUnits }

        // Assert expected expenditure is 5,000 minor units (GH₵50), NOT 85,000 minor units (GH₵850)
        assertEquals("Expenditure must be GH₵50 (5000 minor units)", 5000L, totalExpenditureMinorUnits)
    }
}
