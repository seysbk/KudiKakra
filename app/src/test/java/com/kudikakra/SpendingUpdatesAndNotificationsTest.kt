package com.kudikakra

import com.kudikakra.data.local.dao.DailyBudgetDao
import com.kudikakra.data.local.dao.TransactionDao
import com.kudikakra.data.local.dao.UserPreferencesDao
import com.kudikakra.data.local.entity.DailyBudgetEntity
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.data.repository.DailyBudgetRepository
import com.kudikakra.data.repository.TransactionRepository
import com.kudikakra.data.repository.UserPreferencesRepository
import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetStatus
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import com.kudikakra.domain.processor.ProcessingResult
import com.kudikakra.domain.processor.TransactionProcessor
import com.kudikakra.notification.NotificationEvent
import com.kudikakra.notification.SpendingNotificationHelper
import com.kudikakra.notification.SpendingNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class SpendingUpdatesAndNotificationsTest {

    private lateinit var txnDao: FakeTransactionDao
    private lateinit var budgetDao: FakeDailyBudgetDao
    private lateinit var prefsDao: FakeUserPreferencesDao

    private lateinit var txnRepo: TransactionRepository
    private lateinit var budgetRepo: DailyBudgetRepository
    private lateinit var prefsRepo: UserPreferencesRepository

    private lateinit var processor: TransactionProcessor

    @Before
    fun setUp() {
        txnDao = FakeTransactionDao()
        budgetDao = FakeDailyBudgetDao()
        prefsDao = FakeUserPreferencesDao()

        txnRepo = TransactionRepository(txnDao)
        budgetRepo = DailyBudgetRepository(budgetDao)
        prefsRepo = UserPreferencesRepository(prefsDao)

        processor = TransactionProcessor(txnRepo, prefsRepo)
        SpendingNotificationManager.resetDeduplicationState()
    }

    // ── Phase 12 Tests: Automatic Spending Updates ───────────────────────────

    @Test
    fun `high-confidence expense automatically increases today spending`() = runBlocking {
        val now = System.currentTimeMillis()
        val event = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 50.00 to Supermarket. Transaction ID: 1001.",
            timestampEpochMillis = now
        )

        val result = processor.process(event)
        assertTrue(result is ProcessingResult.SavedAuto)

        val startOfDay = todayStartMillis()
        val endOfDay = tomorrowStartMillis()
        val totalSpent = txnRepo.getExpenseTotal(startOfDay, endOfDay)

        assertEquals(5_000L, totalSpent)
    }

    @Test
    fun `income withdrawal and transfer do not increase spending total`() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Income: Receive GH₵1,000
        val incomeEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have received GH₵ 1000.00 from Boss. Transaction ID: 2001.",
            timestampEpochMillis = now
        )
        processor.process(incomeEvent)

        // 2. Withdrawal: Withdraw GH₵800
        val withdrawalEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Cash Out made for GH₵ 800.00 at Agent. Transaction ID: 2002.",
            timestampEpochMillis = now + 1000
        )
        processor.process(withdrawalEvent)

        // 3. Transfer: Send GH₵200
        val transferEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "You have transferred GH₵ 200.00 to Friend. Transaction ID: 2003.",
            timestampEpochMillis = now + 2000
        )
        processor.process(transferEvent)

        // 4. Genuine Expense: Spend GH₵50
        val expenseEvent = NotificationEvent(
            packageName = "com.mtn.momo",
            title = "MoMo",
            text = "Payment made for GH₵ 50.00 to Food Stand. Transaction ID: 2004.",
            timestampEpochMillis = now + 3000
        )
        processor.process(expenseEvent)

        val totalSpent = txnRepo.getExpenseTotal(todayStartMillis(), tomorrowStartMillis())

        // Expected expenditure is strictly GH₵50.00 (5,000 minor units), NOT GH₵2,050.00
        assertEquals(5_000L, totalSpent)
    }

    @Test
    fun `unknown or low confidence transactions do not increase spending`() = runBlocking {
        val now = System.currentTimeMillis()
        val lowConfEntity = TransactionEntity(
            source = "MTN MoMo",
            amountMinorUnits = 10_000L,
            currency = "GHS",
            type = TransactionType.UNKNOWN,
            direction = TransactionDirection.UNKNOWN,
            merchant = null,
            reference = null,
            timestampEpochMillis = now,
            confidence = ConfidenceLevel.LOW,
            category = null,
            excludedFromSpending = true,
            fingerprint = "fp_low_conf",
            createdAtEpochMillis = now
        )
        txnRepo.insert(lowConfEntity)

        val totalSpent = txnRepo.getExpenseTotal(todayStartMillis(), tomorrowStartMillis())
        assertEquals(0L, totalSpent)
    }

    // ── Phase 13 Tests: Spending Notifications & Thresholds ─────────────────

    @Test
    fun `notification triggers when approaching limit and when exceeded`() {
        val testManager = TestSpendingNotificationManager()

        // 1. Plan = GH₵100 (10,000 minor), Spent = GH₵50 (50%) -> ON_TRACK, no notification
        val summary1 = BudgetEngine.calculate(planMinorUnits = 10_000L, spentMinorUnits = 5_000L, approachingThresholdPercent = 80)
        assertEquals(BudgetStatus.ON_TRACK, summary1.status)
        val notify1 = testManager.evaluateAndNotify(summary1, spendingNotificationsEnabled = true)
        assertFalse(notify1)
        assertEquals(0, testManager.postedNotifications.size)

        // 2. Spent = GH₵85 (85%) -> APPROACHING_LIMIT, triggers notification
        val summary2 = BudgetEngine.calculate(planMinorUnits = 10_000L, spentMinorUnits = 8_500L, approachingThresholdPercent = 80)
        assertEquals(BudgetStatus.APPROACHING_LIMIT, summary2.status)
        val notify2 = testManager.evaluateAndNotify(summary2, spendingNotificationsEnabled = true)
        assertTrue(notify2)
        assertEquals(1, testManager.postedNotifications.size)
        assertEquals(SpendingNotificationManager.NOTIFICATION_ID_APPROACHING, testManager.postedNotifications[0].first)

        // 3. Spent = GH₵90 (90%) -> Still APPROACHING_LIMIT, duplicate suppressed!
        val summary3 = BudgetEngine.calculate(planMinorUnits = 10_000L, spentMinorUnits = 9_000L, approachingThresholdPercent = 80)
        assertEquals(BudgetStatus.APPROACHING_LIMIT, summary3.status)
        val notify3 = testManager.evaluateAndNotify(summary3, spendingNotificationsEnabled = true)
        assertFalse("Duplicate notification for same status must be suppressed", notify3)
        assertEquals(1, testManager.postedNotifications.size)

        // 4. Spent = GH₵110 (110%) -> EXCEEDED, triggers new notification!
        val summary4 = BudgetEngine.calculate(planMinorUnits = 10_000L, spentMinorUnits = 11_000L, approachingThresholdPercent = 80)
        assertEquals(BudgetStatus.EXCEEDED, summary4.status)
        val notify4 = testManager.evaluateAndNotify(summary4, spendingNotificationsEnabled = true)
        assertTrue(notify4)
        assertEquals(2, testManager.postedNotifications.size)
        assertEquals(SpendingNotificationManager.NOTIFICATION_ID_EXCEEDED, testManager.postedNotifications[1].first)
    }

    @Test
    fun `disabled spending notifications suppress all alerts`() {
        val testManager = TestSpendingNotificationManager()
        val summary = BudgetEngine.calculate(planMinorUnits = 10_000L, spentMinorUnits = 12_000L, approachingThresholdPercent = 80)

        assertEquals(BudgetStatus.EXCEEDED, summary.status)
        val notified = testManager.evaluateAndNotify(summary, spendingNotificationsEnabled = false)

        assertFalse(notified)
        assertEquals(0, testManager.postedNotifications.size)
    }

    @Test
    fun `spending notification helper evaluates repository state and notifies`() = runBlocking {
        val testManager = TestSpendingNotificationManager()
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // Set budget = GH₵100
        budgetRepo.save(DailyBudgetEntity(dayOfWeek = todayDay, amountMinorUnits = 10_000L, updatedAtEpochMillis = System.currentTimeMillis()))

        // Set user preferences with threshold = 80%
        prefsRepo.save(UserPreferencesEntity(spendingNotificationsEnabled = true, notificationThresholdPercent = 80))

        val helper = SpendingNotificationHelper(txnRepo, budgetRepo, prefsRepo, testManager)

        // No spending -> No notification
        val result1 = helper.checkAndNotify()
        assertFalse(result1)

        // Spend GH₵85 -> Triggers notification
        val now = System.currentTimeMillis()
        txnRepo.insert(
            TransactionEntity(
                source = "MTN MoMo",
                amountMinorUnits = 8_500L,
                currency = "GHS",
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUT,
                merchant = "Supermarket",
                reference = "123",
                timestampEpochMillis = now,
                confidence = ConfidenceLevel.HIGH,
                category = null,
                excludedFromSpending = false,
                fingerprint = "fp_85",
                createdAtEpochMillis = now
            )
        )

        val result2 = helper.checkAndNotify()
        assertTrue(result2)
        assertEquals(1, testManager.postedNotifications.size)
        assertTrue(testManager.postedNotifications[0].second.contains("Approaching Daily Budget"))
    }

    // ── Helper Fakes and Test Doubles ────────────────────────────────────────

    private class TestSpendingNotificationManager : SpendingNotificationManager() {
        val postedNotifications = mutableListOf<Pair<Int, String>>()

        override fun postNotification(notificationId: Int, title: String, text: String) {
            postedNotifications.add(notificationId to "$title: $text")
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val stored = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override fun observeAll(): Flow<List<TransactionEntity>> = flowOf(stored)

        override fun observePendingReview(): Flow<List<TransactionEntity>> =
            flowOf(stored.filter { it.confidence == ConfidenceLevel.MEDIUM || it.type == TransactionType.UNKNOWN })

        override fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long> =
            flowOf(calculateExpenseTotal(startMillis, endMillis))

        override suspend fun getExpenseTotal(startMillis: Long, endMillis: Long): Long =
            calculateExpenseTotal(startMillis, endMillis)

        private fun calculateExpenseTotal(startMillis: Long, endMillis: Long): Long =
            stored.filter {
                it.timestampEpochMillis in startMillis until endMillis &&
                    it.type == TransactionType.EXPENSE &&
                    !it.excludedFromSpending
            }.sumOf { it.amountMinorUnits }

        override suspend fun insert(transaction: TransactionEntity): Long {
            val id = nextId++
            stored.add(transaction.copy(id = id))
            return id
        }

        override suspend fun update(transaction: TransactionEntity) {
            val idx = stored.indexOfFirst { it.id == transaction.id }
            if (idx != -1) stored[idx] = transaction
        }

        override suspend fun delete(transaction: TransactionEntity) {
            stored.removeAll { it.id == transaction.id }
        }

        override suspend fun deleteAll() {
            stored.clear()
        }
    }

    private class FakeDailyBudgetDao : DailyBudgetDao {
        val budgets = mutableMapOf<Int, DailyBudgetEntity>()

        override fun observeAll(): Flow<List<DailyBudgetEntity>> = flowOf(budgets.values.toList())

        override suspend fun getByDay(dayOfWeek: Int): DailyBudgetEntity? = budgets[dayOfWeek]

        override suspend fun upsert(budget: DailyBudgetEntity) {
            budgets[budget.dayOfWeek] = budget
        }

        override suspend fun deleteAll() {
            budgets.clear()
        }
    }

    private class FakeUserPreferencesDao : UserPreferencesDao {
        private var prefs: UserPreferencesEntity? = UserPreferencesEntity()

        override fun observe(): Flow<UserPreferencesEntity?> = flowOf(prefs)

        override suspend fun getPreferencesSync(): UserPreferencesEntity? = prefs

        override suspend fun upsert(preferences: UserPreferencesEntity) {
            prefs = preferences
        }
    }

    private fun todayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun tomorrowStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}
