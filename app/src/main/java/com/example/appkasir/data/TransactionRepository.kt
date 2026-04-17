package com.example.appkasir.data

import com.example.appkasir.data.local.dao.TransactionDao
import com.example.appkasir.data.local.entity.TransactionEntity
import com.example.appkasir.data.local.entity.TransactionItemEntity
import com.example.appkasir.data.local.entity.TransactionWithItems

data class LocalTransactionItem(
    val name: String,
    val type: String,
    val qty: Double,
    val subtotal: Long
)

object TransactionStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
}

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    suspend fun savePendingTransaction(
        total: Long,
        roundedTotal: Long,
        cashReceived: Long,
        changeAmount: Long,
        items: List<LocalTransactionItem>
    ): Long {
        val transactionId = transactionDao.insertTransaction(
            TransactionEntity(
                total = total,
                roundedTotal = roundedTotal,
                status = TransactionStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                cashReceived = cashReceived,
                changeAmount = changeAmount
            )
        )

        val entities = items.map {
            TransactionItemEntity(
                transactionId = transactionId,
                name = it.name,
                type = it.type,
                qty = it.qty,
                subtotal = it.subtotal
            )
        }

        transactionDao.insertTransactionItems(entities)
        return transactionId
    }

    suspend fun getPendingTransactions(): List<TransactionWithItems> {
        return transactionDao.getTransactionsByStatus(TransactionStatus.PENDING)
    }

    suspend fun getAllTransactions(): List<TransactionWithItems> {
        return transactionDao.getAllTransactions()
    }

    suspend fun getPendingCount(): Int {
        return transactionDao.countByStatus(TransactionStatus.PENDING)
    }

    suspend fun markSynced(transactionId: Long) {
        transactionDao.updateStatus(transactionId, TransactionStatus.SYNCED)
    }
}
