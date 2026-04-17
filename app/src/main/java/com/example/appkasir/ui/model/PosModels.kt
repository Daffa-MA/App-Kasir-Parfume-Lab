package com.example.appkasir.ui.model

import java.text.NumberFormat
import java.util.Locale

data class PerfumeProduct(
    val id: String,
    val name: String,
    val pricePerMl: Long,
    var stockMl: Double
)

data class BottleProduct(
    val id: String,
    val name: String,
    val capacityMl: Int,
    val price: Long,
    var stockPcs: Int
)

sealed class CartItem {
    abstract val name: String
    abstract val type: String
    abstract val qty: Double
    abstract val subtotal: Long
    abstract val detail: String

    data class Perfume(
        val product: PerfumeProduct,
        val ml: Double,
        override val subtotal: Long
    ) : CartItem() {
        override val name: String = product.name
        override val type: String = TYPE_PERFUME
        override val qty: Double = ml
        override val detail: String = "${formatQuantity(ml)} ml"
    }

    data class Bottle(
        val bottle: BottleProduct,
        val pcs: Int,
        override val subtotal: Long
    ) : CartItem() {
        override val name: String = bottle.name
        override val type: String = TYPE_BOTTLE
        override val qty: Double = pcs.toDouble()
        override val detail: String = "$pcs pcs"
    }

    companion object {
        const val TYPE_PERFUME = "perfume"
        const val TYPE_BOTTLE = "bottle"
    }
}

fun formatCurrency(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}

fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.')
    }
}
