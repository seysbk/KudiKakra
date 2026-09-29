package com.kudikakra.data.repository

import com.kudikakra.data.local.dao.UserPreferencesDao
import com.kudikakra.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepository(private val dao: UserPreferencesDao) {
    fun observe(): Flow<UserPreferencesEntity?> = dao.observe()

    suspend fun getPreferencesSync(): UserPreferencesEntity? = dao.getPreferencesSync()

    suspend fun save(preferences: UserPreferencesEntity) = dao.upsert(preferences)
}
