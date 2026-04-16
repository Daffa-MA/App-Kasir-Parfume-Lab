<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PosTransaction extends Model
{
    protected $table = 'transactions';

    protected $fillable = [
        'legacy_id',
        'items',
        'perfume_subtotal',
        'alcohol_subtotal',
        'bottle_subtotal',
        'total',
        'rounded_total',
        'payment_method',
        'cash_received',
        'change_amount',
        'status',
        'created_at',
        'updated_at',
    ];
}
