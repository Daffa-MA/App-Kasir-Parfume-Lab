package com.example.appkasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "total")
    val total: Long,
    @ColumnInfo(name = "rounded_total")
    val roundedTotal: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "cash_received")
    val cashReceived: Long,
    @ColumnInfo(name = "change_amount")
    val changeAmount: Long
)
