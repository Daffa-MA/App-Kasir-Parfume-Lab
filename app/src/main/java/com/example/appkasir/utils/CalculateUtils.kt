package com.example.appkasir.utils

import com.example.appkasir.ui.model.CartItem

data class PosTotals(
    val perfumeSubtotal: Long,
    val alcoholSubtotal: Long,
    val bottleSubtotal: Long,
    val total: Long,
    val roundedTotal: Long,
    val totalBibitMl: Double,
    val totalAlcoholMl: Double
)

object CalculateUtils {
    private const val FREE_ALCOHOL_THRESHOLD_ML = 100.0
    private const val DEFAULT_ALCOHOL_PRICE_PER_ML = 2000.0

    // POS Logic
    fun calculateTotals(
        cartItems: List<CartItem>,
        alcoholPricePerMl: Double = DEFAULT_ALCOHOL_PRICE_PER_ML
    ): PosTotals {
        val perfumeItems = cartItems.filterIsInstance<CartItem.Perfume>()
        val bottleItems = cartItems.filterIsInstance<CartItem.Bottle>()

        val perfumeSubtotal = perfumeItems.sumOf { it.subtotal }
        val bottleSubtotal = bottleItems.sumOf { it.subtotal }
        val totalBibitMl = perfumeItems.sumOf { it.ml }
        val totalAlcoholMl = totalBibitMl

        val alcoholSubtotal = calculateAlcoholSubtotal(
            totalBibitMl = totalBibitMl,
            totalAlcoholMl = totalAlcoholMl,
            alcoholPricePerMl = alcoholPricePerMl
        )

        val total = perfumeSubtotal + alcoholSubtotal + bottleSubtotal
        val roundedTotal = roundToNearestHundred(total)

        return PosTotals(
            perfumeSubtotal = perfumeSubtotal,
            alcoholSubtotal = alcoholSubtotal,
            bottleSubtotal = bottleSubtotal,
            total = total,
            roundedTotal = roundedTotal,
            totalBibitMl = totalBibitMl,
            totalAlcoholMl = totalAlcoholMl
        )
    }

    fun calculateAlcoholSubtotal(
        totalBibitMl: Double,
        totalAlcoholMl: Double,
        alcoholPricePerMl: Double
    ): Long {
        return if (totalBibitMl < FREE_ALCOHOL_THRESHOLD_ML) {
            0L
        } else {
            (alcoholPricePerMl * totalAlcoholMl).toLong()
        }
    }

    fun roundToNearestHundred(value: Long): Long {
        return (Math.round(value / 100.0) * 100)
    }
}
