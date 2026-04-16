package com.example.pos.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ==================== DATA MODELS ====================

data class ApiProductsResponse(
    val success: Boolean,
    val data: ProductsData
)

data class ProductsData(
    val perfumes: List<ApiPerfume>,
    val bottles: List<ApiBottle>
)

data class ApiPerfume(
    val id: String,
    val name: String,
    @SerializedName("price_per_ml") val pricePerMl: Double,
    @SerializedName("stock_ml") val stockMl: Double
)

data class ApiBottle(
    val id: String,
    val name: String,
    val price: Long,
    @SerializedName("capacity_ml") val capacityMl: Int,
    @SerializedName("stock_pcs") val stockPcs: Int
)

data class TransactionRequest(
    val items: List<TransactionItemRequest>,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("cash_received") val cashReceived: Long?
)

data class TransactionItemRequest(
    @SerializedName("product_id") val productId: String,
    val quantity: Double,
    val category: String
)

data class TransactionResponse(
    val success: Boolean,
    val data: TransactionData? = null,
    val error: String? = null
)

data class TransactionData(
    @SerializedName("transaction_id") val transactionId: Long,
    val items: List<TransactionItemResponse>,
    val summary: TransactionSummary,
    @SerializedName("updated_stock") val updatedStock: List<StockUpdate>
)

data class TransactionItemResponse(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    val category: String,
    val quantity: Double,
    val subtotal: Double
)

data class TransactionSummary(
    @SerializedName("perfume_subtotal") val perfumeSubtotal: Double,
    @SerializedName("alcohol_ml") val alcoholMl: Double,
    @SerializedName("alcohol_subtotal") val alcoholSubtotal: Double,
    @SerializedName("bottle_subtotal") val bottleSubtotal: Double,
    val total: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("cash_received") val cashReceived: Long?,
    val change: Double?
)

data class StockUpdate(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("stock_ml") val stockMl: Double,
    @SerializedName("stock_pcs") val stockPcs: Int
)

// ==================== API SERVICE ====================

interface PosApiService {
    @GET("/products")
    fun getProducts(): Call<ApiProductsResponse>

    @POST("/transaction")
    fun createTransaction(@Body request: TransactionRequest): Call<TransactionResponse>
}

// ==================== CLIENT ====================

object ApiClient {
    // 10.0.2.2 = localhost from Android emulator
    // Change to your PC IP if using physical device
    private const val BASE_URL = "http://10.0.2.2:3000/"
    private const val API_TOKEN = "dev-pos-token"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (API_TOKEN.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $API_TOKEN")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    val instance: PosApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PosApiService::class.java)
    }
}
