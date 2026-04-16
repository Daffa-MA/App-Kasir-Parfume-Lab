package com.example.appkasir.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appkasir.data.local.dao.TransactionDao
import com.example.appkasir.data.local.entity.TransactionEntity
import com.example.appkasir.data.local.entity.TransactionItemEntity

@Database(
    entities = [TransactionEntity::class, TransactionItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perfume_lab_pos.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN rounded_total INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE transactions SET rounded_total = total WHERE rounded_total = 0")
            }
        }
    }
}
