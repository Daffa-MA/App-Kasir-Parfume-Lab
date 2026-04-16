<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class TransactionItem extends Model
{
    protected $table = 'transaction_items';

    protected $fillable = [
        'transaction_id',
        'product_id',
        'product_name',
        'category',
        'quantity',
        'unit_price',
        'subtotal',
    ];
}
