package com.example.appkasir.sync

import android.content.Context
import com.example.appkasir.data.TransactionRepository
import com.example.appkasir.data.TransactionStatus
import com.example.appkasir.data.local.AppDatabase
import com.example.appkasir.network.RetrofitClient
import com.example.appkasir.network.SyncBatchTransactionRequest
import com.example.appkasir.network.SyncTransactionHeader
import com.example.appkasir.network.SyncTransactionItem
import com.example.appkasir.network.SyncTransactionRequest
import com.example.appkasir.utils.NetworkUtils

enum class SyncIndicator {
    SYNCED,
    PENDING,
    OFFLINE,
    ERROR,
    LOADING
}

data class SyncUiState(
    val indicator: SyncIndicator,
    val pendingCount: Int,
    val message: String
)

object SyncEngine {
    private fun repository(context: Context): TransactionRepository {
        val dao = AppDatabase.getInstance(context).transactionDao()
        return TransactionRepository(dao)
    }

    suspend fun currentState(context: Context): SyncUiState {
        val repo = repository(context)
        val pendingCount = repo.getPendingCount()

        if (!NetworkUtils.isOnline(context)) {
            return SyncUiState(
                indicator = SyncIndicator.OFFLINE,
                pendingCount = pendingCount,
                message = "Offline"
            )
        }

        return if (pendingCount > 0) {
            SyncUiState(
                indicator = SyncIndicator.PENDING,
                pendingCount = pendingCount,
                message = "Pending sync: $pendingCount"
            )
        } else {
            SyncUiState(
                indicator = SyncIndicator.SYNCED,
                pendingCount = 0,
                message = "Synced"
            )
        }
    }

    // Sync Logic
    suspend fun syncPendingTransactions(context: Context): SyncUiState {
        val repo = repository(context)
        if (!NetworkUtils.isOnline(context)) {
            val pendingCount = repo.getPendingCount()
            return SyncUiState(
                indicator = SyncIndicator.OFFLINE,
                pendingCount = pendingCount,
                message = "Offline, retry later"
            )
        }

        val pendingTransactions = repo.getPendingTransactions()
        if (pendingTransactions.isEmpty()) {
            return SyncUiState(
                indicator = SyncIndicator.SYNCED,
                pendingCount = 0,
                message = "No pending transaction"
            )
        }

        var syncedCount = 0
        var failedCount = 0

        val requests = pendingTransactions.map { pending ->
            SyncTransactionRequest(
                transaction = SyncTransactionHeader(
                    localId = pending.transaction.id,
                    total = pending.transaction.total,
                    roundedTotal = pending.transaction.roundedTotal,
                    createdAt = pending.transaction.createdAt,
                    status = TransactionStatus.PENDING
                ),
                items = pending.items.map {
                    SyncTransactionItem(
                        name = it.name,
                        type = it.type,
                        qty = it.qty,
                        subtotal = it.subtotal
                    )
                }
            )
        }

        try {
            val response = RetrofitClient.apiService.postTransactionBatch(
                SyncBatchTransactionRequest(transactions = requests)
            )

            if (response.isSuccessful) {
                val data = response.body()?.data.orEmpty()

                if (data.isEmpty()) {
                    failedCount = pendingTransactions.size
                } else {
                    data.forEach { result ->
                        val localId = result.local_id
                        if (result.success && localId != null) {
                            repo.markSynced(localId)
                            syncedCount += 1
                        } else {
                            failedCount += 1
                        }
                    }

                    val unreportedCount = pendingTransactions.size - data.size
                    if (unreportedCount > 0) {
                        failedCount += unreportedCount
                    }
                }
            } else {
                failedCount = pendingTransactions.size
            }
        } catch (_: Exception) {
            failedCount = pendingTransactions.size
        }

        val pendingCount = repo.getPendingCount()
        return when {
            pendingCount == 0 -> SyncUiState(
                indicator = SyncIndicator.SYNCED,
                pendingCount = 0,
                message = "Synced $syncedCount transaction(s)"
            )
            failedCount > 0 -> SyncUiState(
                indicator = SyncIndicator.ERROR,
                pendingCount = pendingCount,
                message = "Sync error, $pendingCount still pending"
            )
            else -> SyncUiState(
                indicator = SyncIndicator.PENDING,
                pendingCount = pendingCount,
                message = "$pendingCount pending"
            )
        }
    }
}
