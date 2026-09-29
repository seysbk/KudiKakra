package com.kudikakra.domain.processor

import com.kudikakra.domain.model.NormalizedTransaction
import com.kudikakra.data.local.entity.TransactionEntity
import java.security.MessageDigest
import java.util.Locale

object TransactionFingerprintGenerator {

    private const val FIVE_MINUTES_MILLIS = 5 * 60 * 1000L

    fun generate(transaction: NormalizedTransaction): String {
        return generate(
            source = transaction.source,
            type = transaction.type.name,
            amountMinorUnits = transaction.amountMinorUnits,
            reference = transaction.reference,
            merchant = transaction.merchant,
            timestampEpochMillis = transaction.timestampEpochMillis
        )
    }

    fun generate(entity: TransactionEntity): String {
        return generate(
            source = entity.source,
            type = entity.type.name,
            amountMinorUnits = entity.amountMinorUnits,
            reference = entity.reference,
            merchant = entity.merchant,
            timestampEpochMillis = entity.timestampEpochMillis
        )
    }

    fun generate(
        source: String,
        type: String,
        amountMinorUnits: Long,
        reference: String?,
        merchant: String?,
        timestampEpochMillis: Long
    ): String {
        val normSource = source.trim().lowercase(Locale.ROOT)
        val normType = type.trim().uppercase(Locale.ROOT)
        val normRef = reference?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val normMerchant = merchant?.trim()?.lowercase(Locale.ROOT).orEmpty()

        val rawInput = if (normRef.isNotBlank()) {
            "ref:$normSource:$normType:$amountMinorUnits:$normRef"
        } else {
            val timeBucket = timestampEpochMillis / FIVE_MINUTES_MILLIS
            "bucket:$normSource:$normType:$amountMinorUnits:$normMerchant:$timeBucket"
        }

        return sha256(rawInput)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
