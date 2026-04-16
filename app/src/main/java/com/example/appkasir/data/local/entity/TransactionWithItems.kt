package com.example.appkasir.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithItems(
    @Embedded
    val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transaction_id"
    )
    val items: List<TransactionItemEntity>
)
