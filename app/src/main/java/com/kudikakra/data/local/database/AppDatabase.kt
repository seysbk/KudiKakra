package com.kudikakra.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kudikakra.data.local.KudiKakraConverters
import com.kudikakra.data.local.dao.DailyBudgetDao
import com.kudikakra.data.local.dao.TransactionDao
import com.kudikakra.data.local.dao.UserPreferencesDao
import com.kudikakra.data.local.entity.DailyBudgetEntity
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.local.entity.UserPreferencesEntity

@Database(
    entities = [TransactionEntity::class, DailyBudgetEntity::class, UserPreferencesEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(KudiKakraConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun dailyBudgetDao(): DailyBudgetDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kudikakra.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(
                db: androidx.sqlite.db.SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_preferences (
                        id INTEGER NOT NULL,
                        automaticTrackingEnabled INTEGER NOT NULL,
                        spendingNotificationsEnabled INTEGER NOT NULL,
                        notificationThresholdPercent INTEGER NOT NULL,
                        selectedSources TEXT NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(
                db: androidx.sqlite.db.SupportSQLiteDatabase
            ) {
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN merchantRules TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(
                db: androidx.sqlite.db.SupportSQLiteDatabase
            ) {
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN developerModeEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
