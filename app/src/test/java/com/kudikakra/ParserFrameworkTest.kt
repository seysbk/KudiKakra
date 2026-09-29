package com.kudikakra

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.NormalizedTransaction
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.parser.FinancialParser
import com.kudikakra.domain.parser.ParseResult
import com.kudikakra.domain.parser.ParserRegistry
import com.kudikakra.notification.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserFrameworkTest {

    private val sampleEvent = NotificationEvent(
        packageName = "com.test.momo",
        title = "MoMo Alert",
        text = "Paid GH₵50.00 to Merchant A",
        timestampEpochMillis = 1700000000000L
    )

    // ── NormalizedTransaction Tests ───────────────────────────────────────────

    @Test
    fun `EXPENSE with HIGH confidence is NOT excluded from spending`() {
        val highExpense = NormalizedTransaction(
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            source = "MTN MoMo",
            timestampEpochMillis = System.currentTimeMillis(),
            confidence = ConfidenceLevel.HIGH
        )
        assertFalse(highExpense.excludedFromSpending)
    }

    @Test
    fun `EXPENSE with MEDIUM or LOW confidence IS excluded from spending`() {
        val mediumExpense = NormalizedTransaction(
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            source = "MTN MoMo",
            timestampEpochMillis = System.currentTimeMillis(),
            confidence = ConfidenceLevel.MEDIUM
        )
        assertTrue(mediumExpense.excludedFromSpending)

        val lowExpense = NormalizedTransaction(
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            source = "Unknown",
            timestampEpochMillis = System.currentTimeMillis(),
            confidence = ConfidenceLevel.LOW
        )
        assertTrue(lowExpense.excludedFromSpending)
    }

    @Test
    fun `INCOME, TRANSFER, WITHDRAWAL, and UNKNOWN types ARE excluded from spending`() {
        val types = listOf(
            TransactionType.INCOME,
            TransactionType.TRANSFER,
            TransactionType.WITHDRAWAL,
            TransactionType.UNKNOWN
        )

        for (type in types) {
            val transaction = NormalizedTransaction(
                amountMinorUnits = 10000L,
                type = type,
                direction = if (type == TransactionType.INCOME) TransactionDirection.IN else TransactionDirection.OUT,
                source = "Test Source",
                timestampEpochMillis = System.currentTimeMillis(),
                confidence = ConfidenceLevel.HIGH
            )
            assertTrue(
                "Expected excludedFromSpending to be true for type $type",
                transaction.excludedFromSpending
            )
        }
    }

    // ── ParserRegistry Tests ──────────────────────────────────────────────────

    @Test
    fun `ParserRegistry delegates to matching parser when handleable`() {
        val dummyParser = object : FinancialParser {
            override fun canHandle(event: NotificationEvent): Boolean =
                event.packageName == "com.test.momo"

            override fun parse(event: NotificationEvent): ParseResult {
                val txn = NormalizedTransaction(
                    amountMinorUnits = 5000L,
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.OUT,
                    merchant = "Merchant A",
                    source = "MTN MoMo",
                    timestampEpochMillis = event.timestampEpochMillis,
                    confidence = ConfidenceLevel.HIGH
                )
                return ParseResult(
                    success = true,
                    transaction = txn,
                    confidence = ConfidenceLevel.HIGH,
                    reason = "Successfully parsed MoMo expense"
                )
            }
        }

        val registry = ParserRegistry(listOf(dummyParser))

        val parser = registry.findParser(sampleEvent)
        assertNotNull(parser)

        val result = registry.parse(sampleEvent)
        assertTrue(result.success)
        assertNotNull(result.transaction)
        assertEquals(5000L, result.transaction?.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, result.transaction?.type)
        assertEquals(TransactionDirection.OUT, result.transaction?.direction)
        assertEquals("Merchant A", result.transaction?.merchant)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)
    }

    @Test
    fun `ParserRegistry returns failure result when no parser handles event`() {
        val dummyParser = object : FinancialParser {
            override fun canHandle(event: NotificationEvent): Boolean =
                event.packageName == "com.gcb.bank"

            override fun parse(event: NotificationEvent): ParseResult {
                error("Should not be called")
            }
        }

        val registry = ParserRegistry(listOf(dummyParser))

        val parser = registry.findParser(sampleEvent)
        assertNull(parser)

        val result = registry.parse(sampleEvent)
        assertFalse(result.success)
        assertNull(result.transaction)
        assertEquals(ConfidenceLevel.LOW, result.confidence)
        assertTrue(result.reason.contains("No registered parser can handle"))
    }
}
