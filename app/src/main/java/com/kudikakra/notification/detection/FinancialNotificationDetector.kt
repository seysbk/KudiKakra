package com.kudikakra.notification.detection

import com.kudikakra.notification.NotificationEvent

/**
 * Determines whether a [NotificationEvent] is likely to be a financial
 * transaction notification that is worth handing to the parser pipeline.
 *
 * ## Design principles
 *
 * - **Conservative** — when in doubt, return [DetectionResult.NotFinancial].
 *   A missed notification is less harmful than a false expense.
 * - **Pure / testable** — no Android framework dependencies; all inputs are
 *   plain data.  Unit tests do not require an Android runtime.
 * - **Source-filtered** — only notifications from sources the user has
 *   selected ([enabledSources]) are considered.  Pass an empty set to disable
 *   automatic detection entirely.
 *
 * ## Detection layers (applied in order)
 *
 * 1. **Empty guard** — blank text produces [DetectionResult.NotFinancial] immediately.
 * 2. **Package lookup** — if the package matches a known [FinancialSource], note it.
 * 3. **Source filter** — if a known source was found but the user has not
 *    enabled it, return [DetectionResult.NotFinancial].
 * 4. **Keyword scoring** — scan the combined title + text for weighted keywords
 *    and accumulate a score.
 * 5. **Confidence mapping** — combine the package match and keyword score to
 *    produce the final [FinancialConfidence].
 */
class FinancialNotificationDetector(
    /**
     * The set of financial sources the user has chosen to monitor.
     * Notifications from sources NOT in this set are ignored.
     * Pass an empty set to skip all automatic detection.
     */
    private val enabledSources: Set<FinancialSource> = FinancialSource.entries.toSet(),
) {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Analyses [event] and returns whether it is a financial notification.
     *
     * @param event The raw notification event from the listener service.
     * @return [DetectionResult.Financial] if the notification should be parsed,
     *         [DetectionResult.NotFinancial] otherwise.
     */
    fun detect(event: NotificationEvent): DetectionResult {
        val combinedText = buildSearchText(event)

        // Layer 1 — empty guard
        if (combinedText.isBlank()) {
            return DetectionResult.NotFinancial("Notification text is empty")
        }

        if (NON_FINANCIAL_PHRASES.any(combinedText::contains)) {
            return DetectionResult.NotFinancial("Notification is a non-transaction account update")
        }

        // Layer 2 — known package lookup
        val knownSource = FinancialSource.fromNotification(event.packageName, event.title, event.text)

        // Layer 3 — source filter
        if (knownSource != null && knownSource !in enabledSources) {
            return DetectionResult.NotFinancial(
                "Source '${knownSource.displayName}' is not enabled by the user"
            )
        }

        // Layer 4 — keyword scoring
        val score = scoreKeywords(combinedText)

        // Layer 5 — confidence mapping
        return mapToResult(score = score, knownSource = knownSource, combinedText = combinedText)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Combines title and text into a single lowercase search string. */
    private fun buildSearchText(event: NotificationEvent): String =
        "${event.title} ${event.text}".trim().lowercase()

    /**
     * Scans [text] for financial keywords and returns a total weighted score.
     *
     * Keyword weights:
     * - **+3  (strong)**  — GHS/GH₵ symbols, MoMo, debit, credit, withdrawal,
     *                        cash out, transaction, ATM
     * - **+2  (moderate)** — paid, received, sent, payment, balance, amount,
     *                        transfer, deposit, refund, salary
     * - **+1  (weak)**     — account, fund, money
     */
    private fun scoreKeywords(text: String): Int {
        var score = 0

        for ((keyword, weight) in WEIGHTED_KEYWORDS) {
            if (text.contains(keyword, ignoreCase = true)) {
                score += weight
            }
        }

        return score
    }

    /**
     * Converts a keyword [score] and optional [knownSource] into a
     * [DetectionResult].
     *
     * Confidence rules (conservative — prefer NOT financial when uncertain):
     *
     * | Package known? | Score | Confidence |
     * |---------------|-------|------------|
     * | Yes           | ≥ 3   | HIGH       |
     * | Yes           | ≥ 1   | MEDIUM     |
     * | Yes           | 0     | LOW        |
     * | No            | ≥ 5   | MEDIUM     |
     * | No            | ≥ 3   | LOW        |
     * | No            | < 3   | NotFinancial |
     *
     * A known package alone (score = 0) still warrants LOW confidence because
     * many apps send non-financial notifications.
     */
    private fun mapToResult(score: Int, knownSource: FinancialSource?, combinedText: String): DetectionResult {
        if (knownSource != null) {
            val hasAmount = HAS_AMOUNT_PATTERN.containsMatchIn(combinedText)
            val hasAction = score >= SCORE_LOW_THRESHOLD || hasAmount
            if (!hasAction) {
                return DetectionResult.NotFinancial(
                    "Known financial package lacks a transaction signal or amount value"
                )
            }

            val confidence = when {
                score >= SCORE_HIGH_THRESHOLD -> FinancialConfidence.HIGH
                score >= SCORE_LOW_THRESHOLD -> FinancialConfidence.MEDIUM
                else -> FinancialConfidence.LOW
            }
            return DetectionResult.Financial(
                confidence = confidence,
                matchedSource = knownSource,
                reason = "Known package '${knownSource.displayName}', keyword score=$score",
            )
        }

        when {
            score >= SCORE_UNKNOWN_HIGH -> return DetectionResult.Financial(
                confidence = FinancialConfidence.MEDIUM,
                matchedSource = null,
                reason = "Unknown package, strong keyword score=$score",
            )
            score >= SCORE_UNKNOWN_LOW -> return DetectionResult.Financial(
                confidence = FinancialConfidence.LOW,
                matchedSource = null,
                reason = "Unknown package, moderate keyword score=$score",
            )
            else -> return DetectionResult.NotFinancial(
                "Insufficient financial keyword evidence (score=$score)"
            )
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    private companion object {
        val NON_FINANCIAL_PHRASES = listOf(
            "profile details were updated",
            "profile has been updated",
            "profile updated",
        )

        // Keyword → weight pairs.  The list is ordered: stronger signals first
        // so that sub-string matches don't accidentally double-count.
        val WEIGHTED_KEYWORDS: List<Pair<String, Int>> = listOf(
            // ── Strong (+3) ────────────────────────────────────────────────────
            "gh₵"         to 3,
            "ghs"         to 3,
            "ghc"         to 3,
            "momo"        to 3,
            "debit"       to 3,
            "credit"      to 3,
            "withdrawal"  to 3,
            "withdrawn"   to 3,
            "cash out"    to 3,
            "cash-out"    to 3,
            "cashout"     to 3,
            "transaction" to 3,
            "atm"         to 3,

            // ── Moderate (+2) ──────────────────────────────────────────────────
            "paid"        to 2,
            "received"    to 2,
            "payment"     to 2,
            "balance"     to 2,
            "amount"      to 2,
            "transfer"    to 2,
            "deposit"     to 2,
            "refund"      to 2,
            "salary"      to 2,
            "sent"        to 2,

            // ── Weak (+1) ─────────────────────────────────────────────────────
            "account"     to 1,
            "fund"        to 1,
            "money"       to 1,
        )

        // Thresholds for known-package path
        const val SCORE_HIGH_THRESHOLD = 3
        const val SCORE_LOW_THRESHOLD  = 1

        // Thresholds for unknown-package path (stricter)
        const val SCORE_UNKNOWN_HIGH   = 5
        const val SCORE_UNKNOWN_LOW    = 3

        val HAS_AMOUNT_PATTERN = Regex(
            "(?:gh₵|ghs|ghc|gh)\\s*\\d[\\d,]*(?:\\.\\d{1,2})?|\\d[\\d,]*\\s*(?:gh₵|ghs|ghc|gh)",
            RegexOption.IGNORE_CASE,
        )
    }
}
