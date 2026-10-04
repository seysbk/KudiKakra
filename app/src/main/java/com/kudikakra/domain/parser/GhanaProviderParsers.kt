package com.kudikakra.domain.parser

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.NormalizedTransaction
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.notification.NotificationEvent
import com.kudikakra.notification.detection.FinancialSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class GcbParser : GhanaProviderParser(FinancialSource.GCB, SOURCE_NAME) {
    companion object {
        const val SOURCE_NAME = "GCB Bank"
    }
}

class TelecelCashParser : GhanaProviderParser(FinancialSource.TELECEL_CASH, SOURCE_NAME) {
    companion object {
        const val SOURCE_NAME = "Telecel Cash"
    }
}

abstract class GhanaProviderParser(
    private val source: FinancialSource,
    private val sourceName: String,
) : FinancialParser {

    override fun canHandle(event: NotificationEvent): Boolean {
        if (source.packageNames.contains(event.packageName)) return true
        val searchableText = "${event.title} ${event.text}".lowercase(Locale.ROOT)
        return SMS_PACKAGE_NAMES.contains(event.packageName) &&
            source.notificationKeywords.any(searchableText::contains)
    }

    override fun parse(event: NotificationEvent): ParseResult {
        val text = "${event.title} ${event.text}".trim()
        val lowerText = text.lowercase(Locale.ROOT)
        if (text.isBlank() || FAILURE_TERMS.any(lowerText::contains)) {
            return failure("Failed or non-transaction notification")
        }

        val amountMinorUnits = extractAmount(text)
            ?: return failure("Could not extract a transaction amount")
        val classification = classify(lowerText)
            ?: return failure("Notification does not describe a supported transaction")

        val transaction = NormalizedTransaction(
            amountMinorUnits = amountMinorUnits,
            currency = "GHS",
            type = classification.first,
            direction = classification.second,
            merchant = extractParty(text),
            reference = extractReference(text),
            source = sourceName,
            timestampEpochMillis = event.timestampEpochMillis,
            confidence = ConfidenceLevel.HIGH,
        )
        return ParseResult(
            success = true,
            transaction = transaction,
            confidence = transaction.confidence,
            reason = "Parsed $sourceName ${transaction.type} transaction",
        )
    }

    private fun classify(text: String): Pair<TransactionType, TransactionDirection>? {
        return when {
            WITHDRAWAL_TERMS.any(text::contains) ->
                TransactionType.WITHDRAWAL to TransactionDirection.OUT
            INCOME_TERMS.any(text::contains) ->
                TransactionType.INCOME to TransactionDirection.IN
            TRANSFER_TERMS.any(text::contains) ->
                TransactionType.TRANSFER to TransactionDirection.OUT
            EXPENSE_TERMS.any(text::contains) ->
                TransactionType.EXPENSE to TransactionDirection.OUT
            else -> null
        }
    }

    private fun extractAmount(text: String): Long? {
        val matches = AMOUNT_PATTERNS.flatMap { pattern ->
            pattern.findAll(text).toList()
        }.sortedBy { it.range.first }
        val match = matches.firstOrNull { candidate ->
            val preceding = text.substring(0, candidate.range.first).takeLast(50)
            AUXILIARY_AMOUNT_CONTEXT.containsMatchIn(preceding).not()
        } ?: return null
        val numericValue = match.groupValues.drop(1).firstOrNull(String::isNotBlank)
            ?.replace(",", "") ?: return null
        return runCatching {
            BigDecimal(numericValue)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
                .takeIf { it > 0L }
        }.getOrNull()
    }

    private fun extractParty(text: String): String? {
        MERCHANT_AT_PATTERN.find(text)?.groupValues?.get(1)?.let { merchant ->
            return merchant.trim().trimEnd('.', ',', ';').takeIf(String::isNotBlank)
        }
        val match = PARTY_PATTERN.find(text) ?: return null
        return match.groupValues[1]
            .trim()
            .trimEnd('.', ',', ';')
            .takeIf { it.isNotBlank() }
    }

    private fun extractReference(text: String): String? =
        REFERENCE_PATTERN.find(text)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)

    private fun failure(reason: String) = ParseResult(
        success = false,
        transaction = null,
        confidence = ConfidenceLevel.LOW,
        reason = reason,
    )

    private companion object {
        val SMS_PACKAGE_NAMES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.mms",
        )
        val FAILURE_TERMS = listOf("failed", "declined", "reversed", "cancelled", "unsuccessful")
        val WITHDRAWAL_TERMS = listOf("cash out", "cash-out", "cashout", "withdrawal", "withdrawn")
        val INCOME_TERMS = listOf("credited", "credit alert", "received from", "received gh", "deposit from", "money received")
        val TRANSFER_TERMS = listOf("transferred", "transfer to", "sent to", "you have sent")
        val EXPENSE_TERMS = listOf("debit alert", "debited", "debit transaction", "you paid", "paid gh", "paid to", "payment to", "merchant payment", "purchase")
        val AUXILIARY_AMOUNT_CONTEXT = Regex(
            """(?:fee(?:\s+charged)?|(?:current\s+)?balance|available|limit)\s*[:=]?\s*(?:GH₵|GHS|GHC|GH)?\s*$""",
            RegexOption.IGNORE_CASE,
        )
        val AMOUNT_PATTERNS = listOf(
            Regex("""(?:GH₵|GHS|GHC|GH)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:GH₵|GHS|GHC)""", RegexOption.IGNORE_CASE),
            Regex("""(?:amount|amt)\s*[:=]?\s*(?:GH₵|GHS|GHC|GH)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        )
        val PARTY_PATTERN = Regex(
            """(?:to|from|at|merchant)\s+([A-Za-z0-9][A-Za-z0-9 &'()./-]{0,60}?)(?=\s+(?:on|at|ref(?:erence)?|txn|trans(?:action)?\s*id|balance|fee)\b|[.,;]|$)""",
            RegexOption.IGNORE_CASE,
        )
        val MERCHANT_AT_PATTERN = Regex(
            """(?:purchase|transaction|payment)\s+at\s+([A-Za-z0-9][A-Za-z0-9 &'()/-]{0,60}?)(?=\s+(?:ref(?:erence)?|txn|trans(?:action)\s*id|balance|fee)\b|[.,;]|$)""",
            RegexOption.IGNORE_CASE,
        )
        val REFERENCE_PATTERN = Regex(
            """(?:transaction\s*(?:id|ref(?:erence)?)|txn\s*(?:id|ref)|trans\s*id|ref(?:erence)?(?:\s*(?:no|number|id))?)\s*[:#-]?\s*([A-Za-z0-9/-]+)""",
            RegexOption.IGNORE_CASE,
        )
    }
}