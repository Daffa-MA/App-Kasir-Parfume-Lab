package com.example.appkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bottle_products")
data class BottleProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val capacityMl: Int,
    val price: Long,
    val stockPcs: Int
)