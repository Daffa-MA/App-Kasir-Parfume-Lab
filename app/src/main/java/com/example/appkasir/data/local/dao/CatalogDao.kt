package com.example.appkasir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appkasir.data.local.entity.BottleProductEntity
import com.example.appkasir.data.local.entity.PerfumeProductEntity

@Dao
interface CatalogDao {
    @Query("SELECT * FROM perfume_products ORDER BY id ASC")
    suspend fun getPerfumes(): List<PerfumeProductEntity>

    @Query("SELECT * FROM bottle_products ORDER BY id ASC")
    suspend fun getBottles(): List<BottleProductEntity>

    @Query("SELECT COUNT(*) FROM perfume_products")
    suspend fun countPerfumes(): Int

    @Query("SELECT COUNT(*) FROM bottle_products")
    suspend fun countBottles(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPerfumes(items: List<PerfumeProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBottles(items: List<BottleProductEntity>)

    @Query("UPDATE perfume_products SET stockMl = :stockMl WHERE id = :productId")
    suspend fun updatePerfumeStock(productId: String, stockMl: Double)

    @Query("UPDATE bottle_products SET stockPcs = :stockPcs WHERE id = :productId")
    suspend fun updateBottleStock(productId: String, stockPcs: Int)

    @Query("DELETE FROM perfume_products WHERE id = :perfumeId")
    suspend fun deletePerfumeById(perfumeId: String)

    @Query("DELETE FROM bottle_products WHERE id = :bottleId")
    suspend fun deleteBottleById(bottleId: String)
}