package com.example.appkasir.utils

import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.CartItem
import com.example.appkasir.ui.model.PerfumeProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateUtilsTest {

    @Test
    fun calculateTotals_under100ml_usesBottleRemainderButAlcoholIsFree() {
        val perfume = PerfumeProduct("P001", "Ocean Breeze", 15_000, 500.0)
        val bottle = BottleProduct("B001", "Vial 3ml", 3, 1_500, 200)
        val items = listOf(
            CartItem.Perfume(perfume, 2.0, 30_000),
            CartItem.Bottle(bottle, 1, 1_500)
        )

        val totals = CalculateUtils.calculateTotals(items)

        assertEquals(30_000, totals.perfumeSubtotal)
        assertEquals(0, totals.alcoholSubtotal)
        assertEquals(1_500, totals.bottleSubtotal)
        assertEquals(2.0, totals.totalBibitMl, 0.0)
        assertEquals(1.0, totals.totalAlcoholMl, 0.0)
    }

    @Test
    fun calculateTotals_above100ml_chargesAlcoholForRemainingCapacity() {
        val perfume = PerfumeProduct("P001", "Ocean Breeze", 15_000, 500.0)
        val bottle = BottleProduct("B006", "Bottle Spray 150ml", 150, 7_500, 80)
        val items = listOf(
            CartItem.Perfume(perfume, 120.0, 1_800_000),
            CartItem.Bottle(bottle, 1, 7_500)
        )

        val totals = CalculateUtils.calculateTotals(items)

        assertEquals(1_800_000, totals.perfumeSubtotal)
        assertEquals(60_000, totals.alcoholSubtotal)
        assertEquals(7_500, totals.bottleSubtotal)
        assertEquals(120.0, totals.totalBibitMl, 0.0)
        assertEquals(30.0, totals.totalAlcoholMl, 0.0)
    }
}