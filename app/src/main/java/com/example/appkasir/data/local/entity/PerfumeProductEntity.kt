package com.example.appkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfume_products")
data class PerfumeProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val pricePerMl: Long,
    val stockMl: Double
)