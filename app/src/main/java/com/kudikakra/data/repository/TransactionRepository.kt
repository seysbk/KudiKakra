package com.kudikakra.data.repository

import com.kudikakra.data.local.dao.TransactionDao
import com.kudikakra.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()

    fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long> =
        dao.observeExpenseTotal(startMillis, endMillis)

    suspend fun insert(transaction: TransactionEntity): Long = dao.insert(transaction)

    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)
}
