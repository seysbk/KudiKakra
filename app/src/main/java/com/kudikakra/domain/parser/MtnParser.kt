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

/**
 * Parser specifically designed for MTN Mobile Money (MoMo) notifications in Ghana.
 *
 * Handles:
 * - EXPENSE (e.g. "Payment made for GH₵ 25.00 to Merchant", "You have paid GH₵ 50.00 to Merchant", "Payment of GH₵ 10.00 made to MTN Airtime")
 * - INCOME (e.g. "An amount of GH₵ 200.00 has been received from John", "You have received GH₵ 150.00 from Kwame", "Payment received from Alice for GH₵ 75.00")
 * - TRANSFER (e.g. "You have transferred GH₵ 100.00 to Kofi", "Transferred GH₵ 500.00 to Bank", "An amount of GH₵ 50.00 sent to 0244123456")
 * - WITHDRAWAL (e.g. "Cash Out of GH₵ 300.00 from Agent", "Cash out made for GH₵ 150.00 at ATM", "You have withdrawn GH₵ 200.00")
 * - UNKNOWN (Non-financial, failed, or ambiguous messages)
 */
class MtnParser : FinancialParser {

    override fun canHandle(event: NotificationEvent): Boolean {
        if (FinancialSource.MTN_MOMO.packageNames.contains(event.packageName)) {
            return true
        }

        val combined = "${event.packageName} ${event.title} ${event.text}".lowercase(Locale.ROOT)
        val isSmsApp = SMS_PACKAGE_NAMES.contains(event.packageName) || combined.contains("mms") || combined.contains("messaging") || combined.contains("sms")
        val isMtnTitleOrBody = combined.contains("momo") || combined.contains("mobilemoney") || combined.contains("mobile money") || combined.contains("mtn")

        return isSmsApp && isMtnTitleOrBody
    }

    override fun parse(event: NotificationEvent): ParseResult {
        val fullText = "${event.title} ${event.text}".trim()
        if (fullText.isBlank()) {
            return ParseResult(
                success = false,
                transaction = null,
                confidence = ConfidenceLevel.LOW,
                reason = "Notification content is empty"
            )
        }

        // Check for failure indicators
        if (isFailedTransaction(fullText)) {
            return ParseResult(
                success = false,
                transaction = null,
                confidence = ConfidenceLevel.LOW,
                reason = "Transaction failed or non-financial message"
            )
        }

        val amountMinorUnits = extractAmountMinorUnits(fullText)
        if (amountMinorUnits == null || amountMinorUnits <= 0L) {
            return ParseResult(
                success = false,
                transaction = null,
                confidence = ConfidenceLevel.LOW,
                reason = "Could not extract valid transaction amount"
            )
        }

        val reference = extractReference(fullText)
        val lowerText = fullText.lowercase(Locale.ROOT)

        val (type, direction, confidence, merchant) = classify(fullText, lowerText, amountMinorUnits)

        val transaction = NormalizedTransaction(
            amountMinorUnits = amountMinorUnits,
            currency = "GHS",
            type = type,
            direction = direction,
            merchant = merchant,
            reference = reference,
            source = SOURCE_NAME,
            timestampEpochMillis = event.timestampEpochMillis,
            confidence = confidence
        )

        val isSuccess = type != TransactionType.UNKNOWN

        return ParseResult(
            success = isSuccess,
            transaction = transaction,
            confidence = confidence,
            reason = if (isSuccess) "Parsed MTN MoMo $type transaction" else "Could not classify MTN MoMo transaction type"
        )
    }

    private fun classify(
        fullText: String,
        lowerText: String,
        amountMinorUnits: Long
    ): Classification {
        // 1. WITHDRAWAL
        if (isWithdrawalPattern(lowerText)) {
            val agent = extractPartyAfter(fullText, listOf("from Agent ", "from agent ", "from ", "at "))
            return Classification(
                type = TransactionType.WITHDRAWAL,
                direction = TransactionDirection.OUT,
                confidence = ConfidenceLevel.HIGH,
                merchant = agent
            )
        }

        // 2. INCOME
        if (isIncomePattern(lowerText)) {
            val sender = extractPartyAfter(fullText, listOf("received from ", "payment received from ", "from "))
            return Classification(
                type = TransactionType.INCOME,
                direction = TransactionDirection.IN,
                confidence = ConfidenceLevel.HIGH,
                merchant = sender
            )
        }

        // 3. EXPENSE
        if (isExpensePattern(lowerText)) {
            val merchant = extractExpenseMerchant(fullText)
            val confidence = if (merchant != null) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            return Classification(
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUT,
                confidence = confidence,
                merchant = merchant
            )
        }

        // 4. TRANSFER
        if (isTransferPattern(lowerText)) {
            val recipient = extractPartyAfter(fullText, listOf("transferred to ", "sent to ", "to "))
            return Classification(
                type = TransactionType.TRANSFER,
                direction = TransactionDirection.OUT,
                confidence = ConfidenceLevel.HIGH,
                merchant = recipient
            )
        }

        // Default: UNKNOWN
        return Classification(
            type = TransactionType.UNKNOWN,
            direction = TransactionDirection.UNKNOWN,
            confidence = ConfidenceLevel.LOW,
            merchant = null
        )
    }

    private fun isFailedTransaction(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("failed") ||
            lower.contains("insufficient balance") ||
            lower.contains("declined") ||
            lower.contains("cancelled") ||
            lower.contains("pin was changed") ||
            lower.contains("check out") ||
            lower.contains("dial *170#")
    }

    private fun isWithdrawalPattern(lowerText: String): Boolean {
        return lowerText.contains("cash out") ||
            lowerText.contains("cash-out") ||
            lowerText.contains("cashout") ||
            lowerText.contains("withdrawn") ||
            (lowerText.contains("withdrawal") && !lowerText.contains("failed"))
    }

    private fun isIncomePattern(lowerText: String): Boolean {
        return lowerText.contains("received from") ||
            lowerText.contains("has been received") ||
            lowerText.contains("you have received") ||
            lowerText.contains("payment received") ||
            lowerText.contains("credited with") ||
            lowerText.contains("deposit from")
    }

    private fun isExpensePattern(lowerText: String): Boolean {
        return lowerText.contains("payment made for") ||
            lowerText.contains("you have paid") ||
            lowerText.contains("paid gh") ||
            lowerText.contains("payment of gh") ||
            lowerText.contains("payment to") ||
            lowerText.contains("paid to") ||
            lowerText.contains("purchase of") ||
            lowerText.contains("bought airtime") ||
            lowerText.contains("data bundle")
    }

    private fun isTransferPattern(lowerText: String): Boolean {
        return lowerText.contains("transferred") ||
            lowerText.contains("transfer to") ||
            lowerText.contains("sent to") ||
            lowerText.contains("you have sent")
    }

    private fun extractAmountMinorUnits(text: String): Long? {
        val matches = AMOUNT_REGEX.findAll(text)
        for (match in matches) {
            val numStr = (match.groupValues[1].ifEmpty { match.groupValues[2] }).replace(",", "")
            runCatching {
                val bd = BigDecimal(numStr)
                if (bd > BigDecimal.ZERO) {
                    return bd.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
                }
            }
        }
        return null
    }

    private fun extractReference(text: String): String? {
        val txnMatch = TXN_ID_REGEX.find(text)
        if (txnMatch != null) {
            return txnMatch.groupValues[1].trim()
        }
        val refMatch = REF_REGEX.find(text)
        if (refMatch != null) {
            return refMatch.groupValues[1].trim()
        }
        return null
    }

    private fun extractExpenseMerchant(fullText: String): String? {
        val toMatches = listOf(
            Regex("""(?:paid|payment made for|payment of|payment|made)\s+(?:GH₵|GHS|GHC|GH)?\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([A-Za-z0-9\s]+?)(?:\.|\s+Transaction|\s+Txn|\s+Fee|\s+Reference|\s+Ref|$)""", RegexOption.IGNORE_CASE),
            Regex("""to\s+([A-Za-z0-9\s]+?)(?:\.|\s+Transaction|\s+Txn|\s+Fee|\s+Reference|\s+Ref|$)""", RegexOption.IGNORE_CASE)
        )
        for (regex in toMatches) {
            val match = regex.find(fullText)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank() && name.lowercase(Locale.ROOT) != "you" && name.length > 1) {
                    return name
                }
            }
        }
        return null
    }

    private fun extractPartyAfter(fullText: String, prefixes: List<String>): String? {
        val lower = fullText.lowercase(Locale.ROOT)
        for (prefix in prefixes) {
            val idx = lower.indexOf(prefix.lowercase(Locale.ROOT))
            if (idx != -1) {
                val start = idx + prefix.length
                val substring = fullText.substring(start)
                val delimiters = listOf(".", ",", " Transaction", " Txn", " Fee", " Balance", " Ref", " Current")
                var end = substring.length
                for (delim in delimiters) {
                    val delimIdx = substring.lowercase(Locale.ROOT).indexOf(delim.lowercase(Locale.ROOT))
                    if (delimIdx in 0 until end) {
                        end = delimIdx
                    }
                }
                val result = substring.substring(0, end).trim()
                if (result.isNotBlank()) {
                    return result
                }
            }
        }
        return null
    }

    private data class Classification(
        val type: TransactionType,
        val direction: TransactionDirection,
        val confidence: ConfidenceLevel,
        val merchant: String?
    )

    companion object {
        const val SOURCE_NAME = "MTN MoMo"

        private val SMS_PACKAGE_NAMES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.mms"
        )

        private val AMOUNT_REGEX = Regex(
            """(?:GH₵|GHS|GHC|GH)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:GH₵|GHS|GHC|GHS)""",
            RegexOption.IGNORE_CASE
        )

        private val TXN_ID_REGEX = Regex(
            """(?:Transaction\s*ID|Txn\s*ID|Trans\s*ID):\s*([\w-]+)""",
            RegexOption.IGNORE_CASE
        )

        private val REF_REGEX = Regex(
            """(?:Reference|Ref):\s*([^\.\n,]+)""",
            RegexOption.IGNORE_CASE
        )
    }
}
