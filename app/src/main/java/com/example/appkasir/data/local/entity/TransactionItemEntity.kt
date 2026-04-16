package com.example.appkasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "transaction_id", index = true)
    val transactionId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "qty")
    val qty: Double,
    @ColumnInfo(name = "subtotal")
    val subtotal: Long
)
