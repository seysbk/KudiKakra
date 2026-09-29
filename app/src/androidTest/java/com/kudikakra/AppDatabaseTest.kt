package com.kudikakra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kudikakra.data.local.database.AppDatabase
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun expenseTotalExcludesIncomeAndWithdrawals() = runBlocking {
        database.transactionDao().insert(
            transaction(type = TransactionType.EXPENSE, amountMinorUnits = 5_000, excluded = false)
        )
        database.transactionDao().insert(
            transaction(type = TransactionType.INCOME, amountMinorUnits = 100_000, excluded = true)
        )
        database.transactionDao().insert(
            transaction(type = TransactionType.WITHDRAWAL, amountMinorUnits = 80_000, excluded = true)
        )

        val total = database.transactionDao()
            .observeExpenseTotal(0, Long.MAX_VALUE)
            .first()

        assertEquals(5_000L, total)
    }

    @Test
    fun transactionCanBeUpdatedAndDeleted() = runBlocking {
        val id = database.transactionDao().insert(
            transaction(type = TransactionType.EXPENSE, amountMinorUnits = 2_500, excluded = false)
        )
        val saved = database.transactionDao().observeAll().first().single()
        assertEquals(id, saved.id)

        database.transactionDao().update(saved.copy(amountMinorUnits = 3_500))
        assertEquals(3_500L, database.transactionDao().observeAll().first().single().amountMinorUnits)

        database.transactionDao().delete(saved.copy(amountMinorUnits = 3_500))
        assertEquals(0, database.transactionDao().observeAll().first().size)
    }

    @Test
    fun userPreferencesArePersisted() = runBlocking {
        database.userPreferencesDao().upsert(
            UserPreferencesEntity(
                automaticTrackingEnabled = true,
                spendingNotificationsEnabled = false,
                notificationThresholdPercent = 75,
                selectedSources = "MTN,GCB"
            )
        )

        val saved = database.userPreferencesDao().observe().first()

        assertEquals(true, saved?.automaticTrackingEnabled)
        assertEquals(false, saved?.spendingNotificationsEnabled)
        assertEquals(75, saved?.notificationThresholdPercent)
        assertEquals("MTN,GCB", saved?.selectedSources)
    }

    private fun transaction(
        type: TransactionType,
        amountMinorUnits: Long,
        excluded: Boolean
    ) = TransactionEntity(
        source = "test",
        amountMinorUnits = amountMinorUnits,
        type = type,
        direction = if (type == TransactionType.INCOME) {
            TransactionDirection.IN
        } else {
            TransactionDirection.OUT
        },
        timestampEpochMillis = 1_000,
        confidence = ConfidenceLevel.HIGH,
        excludedFromSpending = excluded,
        createdAtEpochMillis = 1_000
    )
}
