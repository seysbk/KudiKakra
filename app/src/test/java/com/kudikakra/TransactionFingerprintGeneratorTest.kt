package com.kudikakra

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.NormalizedTransaction
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.processor.TransactionFingerprintGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TransactionFingerprintGeneratorTest {

    @Test
    fun `identical transactions with reference produce identical fingerprint`() {
        val txn1 = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 2500L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Groceries Store",
            reference = "10293847561",
            timestampEpochMillis = 1000000L,
            confidence = ConfidenceLevel.HIGH
        )

        val txn2 = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 2500L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Groceries Store",
            reference = "10293847561",
            timestampEpochMillis = 1000000L,
            confidence = ConfidenceLevel.HIGH
        )

        val fp1 = TransactionFingerprintGenerator.generate(txn1)
        val fp2 = TransactionFingerprintGenerator.generate(txn2)

        assertEquals("Fingerprints for identical transactions must match", fp1, fp2)
    }

    @Test
    fun `delayed notification with reference produces identical fingerprint despite time difference`() {
        val txnOriginal = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Supermarket",
            reference = "TXN998877",
            timestampEpochMillis = 1000000L,
            confidence = ConfidenceLevel.HIGH
        )

        // Arrives 3 hours later
        val txnDelayed = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Supermarket",
            reference = "TXN998877",
            timestampEpochMillis = 1000000L + (3 * 3600 * 1000L),
            confidence = ConfidenceLevel.HIGH
        )

        val fp1 = TransactionFingerprintGenerator.generate(txnOriginal)
        val fp2 = TransactionFingerprintGenerator.generate(txnDelayed)

        assertEquals("Delayed transaction with reference ID must produce identical fingerprint", fp1, fp2)
    }

    @Test
    fun `repeated notification without reference within 5 minutes produces identical fingerprint`() {
        val baseTime = 1700000000000L
        val txn1 = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 1500L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Taxi Driver",
            reference = null,
            timestampEpochMillis = baseTime,
            confidence = ConfidenceLevel.MEDIUM
        )

        val txn2 = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 1500L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Taxi Driver",
            reference = null,
            timestampEpochMillis = baseTime + 30000L, // 30s later
            confidence = ConfidenceLevel.MEDIUM
        )

        val fp1 = TransactionFingerprintGenerator.generate(txn1)
        val fp2 = TransactionFingerprintGenerator.generate(txn2)

        assertEquals("Repeated notification without reference within time bucket must produce identical fingerprint", fp1, fp2)
    }

    @Test
    fun `different amounts produce different fingerprints`() {
        val txn1 = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 1000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Airtime",
            reference = "REF123",
            timestampEpochMillis = 1000000L,
            confidence = ConfidenceLevel.HIGH
        )

        val txn2 = NormalizedTransaction(
            source = "MTN MoMo",
            amountMinorUnits = 2000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUT,
            merchant = "Airtime",
            reference = "REF123",
            timestampEpochMillis = 1000000L,
            confidence = ConfidenceLevel.HIGH
        )

        val fp1 = TransactionFingerprintGenerator.generate(txn1)
        val fp2 = TransactionFingerprintGenerator.generate(txn2)

        assertNotEquals("Transactions with different amounts must have different fingerprints", fp1, fp2)
    }
}
