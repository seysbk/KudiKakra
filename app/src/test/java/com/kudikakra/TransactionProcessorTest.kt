package com.kudikakra

import com.kudikakra.data.local.dao.TransactionDao
import com.kudikakra.data.local.dao.UserPreferencesDao
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.processor.ProcessingResult
import com.kudikakra.domain.processor.TransactionProcessor
import com.kudikakra.notification.NotificationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionProcessorTest {

    private lateinit var fakeTxnDao: FakeTransactionDao
    private lateinit var fakePrefsDao: FakeUserPreferencesDao
    private lateinit var txnRepo: TransactionRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var processor: TransactionProcessor

    @Before
    fun setUp() {
        fakeTxnDao = FakeTransactionDao()
        fakePrefsDao = FakeUserPreferencesDao()
        txnRepo = TransactionRepository(fakeTxnDao)
        prefsRepo = UserPreferencesRepository(fakePrefsDao)
        processor = TransactionProcessor(txnRepo, prefsRepo)
    }

    @Test
    fun `HIGH confidence notification auto-saves and is included in spending`() = runBlocking {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 50.00 to Supermarket. Transaction ID: 12345678.",
            timestampEpochMillis = 1000000L
        )

        val result = processor.process(event)

        assertTrue(result is ProcessingResult.SavedAuto)
        val saved = (result as ProcessingResult.SavedAuto).entity
        assertEquals(ConfidenceLevel.HIGH, saved.confidence)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertFalse("High confidence expense must NOT be excluded from spending", saved.excludedFromSpending)
        assertEquals(1, fakeTxnDao.storedEntities.size)
    }

    @Test
    fun `MEDIUM confidence notification is saved for review and excluded from spending`() = runBlocking {
        // Expense message missing merchant name -> produces MEDIUM confidence
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 25.00. Transaction ID: 87654321.",
            timestampEpochMillis = 1000000L
        )

        val result = processor.process(event)

        assertTrue(result is ProcessingResult.SavedForReview)
        val saved = (result as ProcessingResult.SavedForReview).entity
        assertEquals(ConfidenceLevel.MEDIUM, saved.confidence)
        assertTrue("Medium confidence expense MUST be excluded from spending until confirmed", saved.excludedFromSpending)
    }

    @Test
    fun `duplicate notification is detected and rejected`() = runBlocking {
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 50.00 to Supermarket. Transaction ID: 12345678.",
            timestampEpochMillis = 1000000L
        )

        val firstResult = processor.process(event)
        assertTrue(firstResult is ProcessingResult.SavedAuto)

        val duplicateResult = processor.process(event)
        assertTrue("Duplicate notification must return ProcessingResult.Duplicate", duplicateResult is ProcessingResult.Duplicate)
        assertEquals(1, fakeTxnDao.storedEntities.size)
    }

    @Test
    fun `deterministic merchant rule applies user learned classification`() = runBlocking {
        // Save user learned rule: "taxi driver" -> EXPENSE
        val prefs = UserPreferencesEntity().withMerchantRule("taxi driver", TransactionType.EXPENSE)
        prefsRepo.save(prefs)

        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have transferred GH₵ 30.00 to Taxi Driver. Transaction ID: 999888.",
            timestampEpochMillis = 1000000L
        )

        // Normally "transferred to" is TRANSFER, but learned merchant rule overrides it to EXPENSE
        val result = processor.process(event)

        assertTrue(result is ProcessingResult.SavedAuto)
        val saved = (result as ProcessingResult.SavedAuto).entity
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(ConfidenceLevel.HIGH, saved.confidence)
        assertFalse("Learned expense must not be excluded from spending", saved.excludedFromSpending)
    }

    private class FakeTransactionDao : TransactionDao {
        val storedEntities = mutableListOf<TransactionEntity>()
        val fingerprints = mutableSetOf<String>()
        private var nextId = 1L

        override fun observeAll(): Flow<List<TransactionEntity>> = flowOf(storedEntities)

        override fun observePendingReview(): Flow<List<TransactionEntity>> =
            flowOf(storedEntities.filter { it.confidence == ConfidenceLevel.MEDIUM || it.type == TransactionType.UNKNOWN })

        override fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long> {
            val sum = storedEntities
                .filter { it.timestampEpochMillis in startMillis until endMillis && it.type == TransactionType.EXPENSE && !it.excludedFromSpending }
                .sumOf { it.amountMinorUnits }
            return flowOf(sum)
        }

        override suspend fun getExpenseTotal(startMillis: Long, endMillis: Long): Long {
            return storedEntities
                .filter { it.timestampEpochMillis in startMillis until endMillis && it.type == TransactionType.EXPENSE && !it.excludedFromSpending }
                .sumOf { it.amountMinorUnits }
        }

        override suspend fun insert(transaction: TransactionEntity): Long {
            val fp = transaction.fingerprint
            if (fp != null && fingerprints.contains(fp)) {
                return -1L
            }
            if (fp != null) fingerprints.add(fp)
            val id = nextId++
            storedEntities.add(transaction.copy(id = id))
            return id
        }

        override suspend fun update(transaction: TransactionEntity) {
            val idx = storedEntities.indexOfFirst { it.id == transaction.id }
            if (idx != -1) storedEntities[idx] = transaction
        }

        override suspend fun delete(transaction: TransactionEntity) {
            storedEntities.removeAll { it.id == transaction.id }
        }

        override suspend fun deleteAll() {
            storedEntities.clear()
            fingerprints.clear()
        }
    }

    private class FakeUserPreferencesDao : UserPreferencesDao {
        private var prefs: UserPreferencesEntity? = null

        override fun observe(): Flow<UserPreferencesEntity?> = flowOf(prefs)

        override suspend fun getPreferencesSync(): UserPreferencesEntity? = prefs

        override suspend fun upsert(preferences: UserPreferencesEntity) {
            prefs = preferences
        }
    }
}
