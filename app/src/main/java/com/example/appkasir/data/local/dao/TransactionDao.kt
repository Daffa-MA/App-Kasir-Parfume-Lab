package com.example.appkasir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.appkasir.data.local.entity.TransactionEntity
import com.example.appkasir.data.local.entity.TransactionItemEntity
import com.example.appkasir.data.local.entity.TransactionWithItems

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Transaction
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY created_at ASC")
    suspend fun getTransactionsByStatus(status: String): List<TransactionWithItems>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("UPDATE transactions SET status = :status WHERE id = :transactionId")
    suspend fun updateStatus(transactionId: Long, status: String)
}
