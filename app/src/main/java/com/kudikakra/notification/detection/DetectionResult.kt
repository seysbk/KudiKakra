package com.kudikakra.notification.detection

/**
 * The output of [FinancialNotificationDetector].
 *
 * Two possible outcomes:
 *
 * - [Financial]    — the notification is likely from a financial service and
 *                    should be passed to the parser pipeline.
 *
 * - [NotFinancial] — the notification should be silently ignored; no parsing
 *                    or storage should occur.
 */
sealed class DetectionResult {

    /**
     * The notification appears to be a financial event worth parsing.
     *
     * @param confidence How confident the detector is.
     * @param matchedSource The known provider if the package was recognised,
     *   or null when the provider is unknown but the content looked financial.
     * @param reason A short, human-readable explanation (for logging/debug only).
     */
    data class Financial(
        val confidence: FinancialConfidence,
        val matchedSource: FinancialSource?,
        val reason: String,
    ) : DetectionResult()

    /**
     * The notification is not financial and should be ignored.
     *
     * @param reason A short, human-readable explanation (for logging/debug only).
     */
    data class NotFinancial(val reason: String) : DetectionResult()
}

/**
 * How confident the detector is that a notification is financial.
 *
 * HIGH   — known provider package + strong financial keywords present.
 * MEDIUM — either a known package or strong keywords, but not both.
 * LOW    — weak signals only; the parser may still attempt processing but
 *          should apply its own caution.
 */
enum class FinancialConfidence {
    HIGH,
    MEDIUM,
    LOW,
}
