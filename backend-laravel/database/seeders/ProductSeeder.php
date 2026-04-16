<?php

namespace Database\Seeders;

use App\Models\Product;
use Illuminate\Database\Seeder;

class ProductSeeder extends Seeder
{
    public function run(): void
    {
        $products = [
            ['id' => 'P001', 'name' => 'Ocean Breeze', 'category' => 'perfume', 'price_per_ml' => 15000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 500, 'stock_pcs' => 0],
            ['id' => 'P002', 'name' => 'Rose Delight', 'category' => 'perfume', 'price_per_ml' => 20000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 350, 'stock_pcs' => 0],
            ['id' => 'P003', 'name' => 'Lavender Mist', 'category' => 'perfume', 'price_per_ml' => 18000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 600, 'stock_pcs' => 0],
            ['id' => 'P004', 'name' => 'Citrus Fresh', 'category' => 'perfume', 'price_per_ml' => 12000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 800, 'stock_pcs' => 0],
            ['id' => 'P005', 'name' => 'Vanilla Dream', 'category' => 'perfume', 'price_per_ml' => 25000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 200, 'stock_pcs' => 0],
            ['id' => 'P006', 'name' => 'Oud Royal', 'category' => 'perfume', 'price_per_ml' => 50000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 150, 'stock_pcs' => 0],
            ['id' => 'P007', 'name' => 'Jasmine Bloom', 'category' => 'perfume', 'price_per_ml' => 22000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 400, 'stock_pcs' => 0],
            ['id' => 'P008', 'name' => 'Sandalwood Pure', 'category' => 'perfume', 'price_per_ml' => 30000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 250, 'stock_pcs' => 0],
            ['id' => 'P009', 'name' => 'Musk Amber', 'category' => 'perfume', 'price_per_ml' => 35000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 180, 'stock_pcs' => 0],
            ['id' => 'P010', 'name' => 'Green Tea', 'category' => 'perfume', 'price_per_ml' => 10000, 'price' => 0, 'capacity_ml' => 0, 'stock_ml' => 1000, 'stock_pcs' => 0],
            ['id' => 'B001', 'name' => 'Vial 3ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 1500, 'capacity_ml' => 3, 'stock_ml' => 0, 'stock_pcs' => 200],
            ['id' => 'B002', 'name' => 'Vial 5ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 2000, 'capacity_ml' => 5, 'stock_ml' => 0, 'stock_pcs' => 200],
            ['id' => 'B003', 'name' => 'Vial 10ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 3000, 'capacity_ml' => 10, 'stock_ml' => 0, 'stock_pcs' => 150],
            ['id' => 'B004', 'name' => 'Botol Spray 15ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 5000, 'capacity_ml' => 15, 'stock_ml' => 0, 'stock_pcs' => 100],
            ['id' => 'B005', 'name' => 'Botol Spray 30ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 7500, 'capacity_ml' => 30, 'stock_ml' => 0, 'stock_pcs' => 80],
            ['id' => 'B006', 'name' => 'Botol Roll On 5ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 2500, 'capacity_ml' => 5, 'stock_ml' => 0, 'stock_pcs' => 120],
            ['id' => 'B007', 'name' => 'Botol Roll On 10ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 3500, 'capacity_ml' => 10, 'stock_ml' => 0, 'stock_pcs' => 100],
            ['id' => 'B008', 'name' => 'Botol Atomizer 20ml', 'category' => 'bottle', 'price_per_ml' => 0, 'price' => 6000, 'capacity_ml' => 20, 'stock_ml' => 0, 'stock_pcs' => 90],
        ];

        foreach ($products as $product) {
            Product::query()->updateOrCreate(['id' => $product['id']], $product);
        }
    }
}
