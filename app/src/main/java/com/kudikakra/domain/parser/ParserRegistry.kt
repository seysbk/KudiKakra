package com.kudikakra.domain.parser

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.notification.NotificationEvent

class ParserRegistry(
    private val parsers: List<FinancialParser> = emptyList()
) {
    fun findParser(event: NotificationEvent): FinancialParser? =
        parsers.firstOrNull { it.canHandle(event) }

    fun parse(event: NotificationEvent): ParseResult {
        val parser = findParser(event)
            ?: return ParseResult(
                success = false,
                transaction = null,
                confidence = ConfidenceLevel.LOW,
                reason = "No registered parser can handle notification from package '${event.packageName}'"
            )
        return parser.parse(event)
    }
}
