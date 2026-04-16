<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Product;
use Illuminate\Http\JsonResponse;

class ProductController extends Controller
{
    public function index(): JsonResponse
    {
        $products = Product::query()
            ->orderBy('category')
            ->orderBy('name')
            ->get();

        $perfumes = $products
            ->where('category', 'perfume')
            ->map(fn (Product $product) => [
                'id' => $product->id,
                'name' => $product->name,
                'price_per_ml' => (float) $product->price_per_ml,
                'stock_ml' => (float) $product->stock_ml,
            ])
            ->values();

        $bottles = $products
            ->where('category', 'bottle')
            ->map(fn (Product $product) => [
                'id' => $product->id,
                'name' => $product->name,
                'price' => (float) $product->price,
                'capacity_ml' => (int) $product->capacity_ml,
                'stock_pcs' => (int) $product->stock_pcs,
            ])
            ->values();

        return response()->json([
            'success' => true,
            'data' => [
                'perfumes' => $perfumes,
                'bottles' => $bottles,
            ],
        ]);
    }
}
