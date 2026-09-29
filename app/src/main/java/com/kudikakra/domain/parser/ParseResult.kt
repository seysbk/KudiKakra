package com.kudikakra.domain.parser

import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.NormalizedTransaction

data class ParseResult(
    val success: Boolean,
    val transaction: NormalizedTransaction? = null,
    val confidence: ConfidenceLevel = transaction?.confidence ?: ConfidenceLevel.LOW,
    val reason: String
)
