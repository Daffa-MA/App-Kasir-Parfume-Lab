package com.example.appkasir.data

import com.example.appkasir.data.local.dao.CatalogDao
import com.example.appkasir.data.local.entity.BottleProductEntity
import com.example.appkasir.data.local.entity.PerfumeProductEntity
import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.PerfumeProduct

class CatalogRepository(
    private val catalogDao: CatalogDao
) {
    suspend fun ensureSeeded() {
        if (catalogDao.countPerfumes() == 0) {
            catalogDao.upsertPerfumes(defaultPerfumeEntities())
        }
        if (catalogDao.countBottles() == 0) {
            catalogDao.upsertBottles(defaultBottleEntities())
        }
    }

    suspend fun getPerfumes(): List<PerfumeProduct> {
        return catalogDao.getPerfumes().map { it.toDomain() }
    }

    suspend fun getBottles(): List<BottleProduct> {
        return catalogDao.getBottles().map { it.toDomain() }
    }

    suspend fun saveCurrentStock(perfumes: List<PerfumeProduct>, bottles: List<BottleProduct>) {
        catalogDao.upsertPerfumes(perfumes.map { it.toEntity() })
        catalogDao.upsertBottles(bottles.map { it.toEntity() })
    }

    private fun PerfumeProductEntity.toDomain(): PerfumeProduct {
        return PerfumeProduct(id = id, name = name, pricePerMl = pricePerMl, stockMl = stockMl)
    }

    private fun BottleProductEntity.toDomain(): BottleProduct {
        return BottleProduct(id = id, name = name, capacityMl = capacityMl, price = price, stockPcs = stockPcs)
    }

    private fun PerfumeProduct.toEntity(): PerfumeProductEntity {
        return PerfumeProductEntity(id = id, name = name, pricePerMl = pricePerMl, stockMl = stockMl)
    }

    private fun BottleProduct.toEntity(): BottleProductEntity {
        return BottleProductEntity(id = id, name = name, capacityMl = capacityMl, price = price, stockPcs = stockPcs)
    }

    private fun defaultPerfumeEntities(): List<PerfumeProductEntity> {
        return listOf(
            PerfumeProductEntity("P001", "Ocean Breeze", 15000, 500.0),
            PerfumeProductEntity("P002", "Rose Delight", 20000, 350.0),
            PerfumeProductEntity("P003", "Lavender Mist", 18000, 600.0),
            PerfumeProductEntity("P004", "Citrus Fresh", 12000, 800.0),
            PerfumeProductEntity("P005", "Vanilla Dream", 25000, 200.0),
            PerfumeProductEntity("P006", "Oud Royal", 50000, 150.0)
        )
    }

    private fun defaultBottleEntities(): List<BottleProductEntity> {
        return listOf(
            BottleProductEntity("B001", "Vial 3ml", 3, 1500, 200),
            BottleProductEntity("B002", "Vial 5ml", 5, 2000, 200),
            BottleProductEntity("B003", "Vial 10ml", 10, 3000, 150),
            BottleProductEntity("B004", "Bottle Spray 15ml", 15, 5000, 100),
            BottleProductEntity("B005", "Bottle Spray 30ml", 30, 7500, 80)
        )
    }
}