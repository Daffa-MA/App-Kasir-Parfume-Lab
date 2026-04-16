<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('transactions', function (Blueprint $table) {
            $table->id();
            $table->text('items');
            $table->double('perfume_subtotal')->default(0);
            $table->double('alcohol_subtotal')->default(0);
            $table->double('bottle_subtotal')->default(0);
            $table->double('total');
            $table->string('payment_method');
            $table->double('cash_received')->default(0);
            $table->double('change_amount')->default(0);
            $table->string('status')->default('SYNCED');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('transactions');
    }
};
