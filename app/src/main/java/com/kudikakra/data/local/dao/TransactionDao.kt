package com.kudikakra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kudikakra.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinorUnits), 0)
        FROM transactions
        WHERE timestampEpochMillis >= :startMillis
          AND timestampEpochMillis < :endMillis
          AND type = 'EXPENSE'
          AND excludedFromSpending = 0
        """
    )
    fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
