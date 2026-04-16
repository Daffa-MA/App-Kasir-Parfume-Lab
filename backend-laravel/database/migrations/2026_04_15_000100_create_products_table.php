<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('products', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('name');
            $table->string('category');
            $table->double('price_per_ml')->default(0);
            $table->double('price')->default(0);
            $table->integer('capacity_ml')->default(0);
            $table->double('stock_ml')->default(0);
            $table->integer('stock_pcs')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('products');
    }
};
