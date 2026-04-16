package com.example.appkasir.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SyncTransactionHeader(
    val localId: Long,
    val total: Long,
    val roundedTotal: Long,
    val createdAt: Long,
    val status: String
)

data class SyncTransactionItem(
    val name: String,
    val type: String,
    val qty: Double,
    val subtotal: Long
)

data class SyncTransactionRequest(
    val transaction: SyncTransactionHeader,
    val items: List<SyncTransactionItem>
)

data class SyncTransactionResponse(
    val success: Boolean = true,
    val message: String? = null
)

data class SyncBatchTransactionRequest(
    val transactions: List<SyncTransactionRequest>
)

data class SyncBatchResult(
    val local_id: Long? = null,
    val success: Boolean = false,
    val error: String? = null
)

data class SyncBatchResponse(
    val success: Boolean = false,
    val data: List<SyncBatchResult> = emptyList()
)

interface ApiService {
    @POST("transaction")
    suspend fun postTransaction(
        @Body request: SyncTransactionRequest
    ): Response<SyncTransactionResponse>

    @POST("transaction")
    suspend fun postTransactionBatch(
        @Body request: SyncBatchTransactionRequest
    ): Response<SyncBatchResponse>
}
