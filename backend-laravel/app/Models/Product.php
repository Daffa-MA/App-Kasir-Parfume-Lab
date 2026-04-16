<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Product extends Model
{
    protected $table = 'products';

    protected $primaryKey = 'id';

    public $incrementing = false;

    protected $keyType = 'string';

    protected $fillable = [
        'id',
        'name',
        'category',
        'price_per_ml',
        'price',
        'capacity_ml',
        'stock_ml',
        'stock_pcs',
    ];
}
