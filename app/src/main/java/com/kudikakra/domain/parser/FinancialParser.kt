package com.kudikakra.domain.parser

import com.kudikakra.notification.NotificationEvent

interface FinancialParser {
    /**
     * Determines whether this parser can handle the given [event].
     */
    fun canHandle(event: NotificationEvent): Boolean

    /**
     * Parses [event] into a [ParseResult].
     */
    fun parse(event: NotificationEvent): ParseResult
}
