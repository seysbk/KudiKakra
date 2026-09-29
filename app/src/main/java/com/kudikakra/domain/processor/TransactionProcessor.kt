package com.kudikakra.domain.processor

import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.NormalizedTransaction
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.parser.ParseResult
import com.kudikakra.domain.parser.ParserRegistry
import com.kudikakra.notification.NotificationEvent
import java.util.Locale

sealed class ProcessingResult {
    data class SavedAuto(val entity: TransactionEntity) : ProcessingResult()
    data class SavedForReview(val entity: TransactionEntity) : ProcessingResult()
    data class HeldLowConfidence(val entity: TransactionEntity) : ProcessingResult()
    data class Duplicate(val fingerprint: String) : ProcessingResult()
    data class NotParsed(val reason: String) : ProcessingResult()
}

class TransactionProcessor(
    private val repository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val parserRegistry: ParserRegistry = ParserRegistry()
) {
    suspend fun process(event: NotificationEvent): ProcessingResult {
        val parseResult = parserRegistry.parse(event)
        val prefs = userPreferencesRepository.getPreferencesSync()
        val merchantRules = prefs?.getMerchantRules() ?: emptyMap()

        val parsedTransaction = parseResult.transaction
            ?: return if (parseResult.confidence == ConfidenceLevel.LOW) {
                handleUnknownOrLowConfidence(event, parseResult)
            } else {
                ProcessingResult.NotParsed(parseResult.reason)
            }

        // Apply safe deterministic merchant rules if a matching user correction exists
        val merchantKey = parsedTransaction.merchant?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val effectiveTransaction = if (merchantKey.isNotBlank() && merchantRules.containsKey(merchantKey)) {
            val ruleType = merchantRules.getValue(merchantKey)
            val learnedConfidence = ConfidenceLevel.HIGH
            parsedTransaction.copy(
                type = ruleType,
                confidence = learnedConfidence,
                excludedFromSpending = NormalizedTransaction.isDefaultExcluded(ruleType, learnedConfidence)
            )
        } else {
            parsedTransaction
        }

        val fingerprint = TransactionFingerprintGenerator.generate(effectiveTransaction)

        val entity = TransactionEntity(
            source = effectiveTransaction.source,
            amountMinorUnits = effectiveTransaction.amountMinorUnits,
            currency = effectiveTransaction.currency,
            type = effectiveTransaction.type,
            direction = effectiveTransaction.direction,
            merchant = effectiveTransaction.merchant,
            reference = effectiveTransaction.reference,
            timestampEpochMillis = effectiveTransaction.timestampEpochMillis,
            confidence = effectiveTransaction.confidence,
            category = null,
            excludedFromSpending = effectiveTransaction.excludedFromSpending,
            fingerprint = fingerprint,
            createdAtEpochMillis = System.currentTimeMillis()
        )

        val rowId = repository.insert(entity)
        if (rowId == -1L) {
            return ProcessingResult.Duplicate(fingerprint)
        }

        return when (effectiveTransaction.confidence) {
            ConfidenceLevel.HIGH -> ProcessingResult.SavedAuto(entity)
            ConfidenceLevel.MEDIUM -> ProcessingResult.SavedForReview(entity)
            ConfidenceLevel.LOW -> ProcessingResult.HeldLowConfidence(entity)
        }
    }

    private suspend fun handleUnknownOrLowConfidence(
        event: NotificationEvent,
        parseResult: ParseResult
    ): ProcessingResult {
        val fingerprint = TransactionFingerprintGenerator.generate(
            source = "Unknown",
            type = TransactionType.UNKNOWN.name,
            amountMinorUnits = 0L,
            reference = null,
            merchant = event.title,
            timestampEpochMillis = event.timestampEpochMillis
        )

        val entity = TransactionEntity(
            source = "Unknown",
            amountMinorUnits = 0L,
            currency = "GHS",
            type = TransactionType.UNKNOWN,
            direction = com.kudikakra.domain.model.TransactionDirection.UNKNOWN,
            merchant = event.title.ifBlank { null },
            reference = null,
            timestampEpochMillis = event.timestampEpochMillis,
            confidence = ConfidenceLevel.LOW,
            category = null,
            excludedFromSpending = true,
            fingerprint = fingerprint,
            createdAtEpochMillis = System.currentTimeMillis()
        )

        val rowId = repository.insert(entity)
        return if (rowId == -1L) {
            ProcessingResult.Duplicate(fingerprint)
        } else {
            ProcessingResult.HeldLowConfidence(entity)
        }
    }
}
