<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Shift extends Model
{
    protected $table = 'shifts';

    protected $fillable = [
        'user_id',
        'opened_at',
        'closed_at',
        'initial_cash',
        'total_sales',
    ];
}
