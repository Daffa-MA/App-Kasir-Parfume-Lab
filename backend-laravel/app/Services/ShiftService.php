<?php

namespace App\Services;

use App\Models\PosTransaction;
use App\Models\Shift;
use Illuminate\Validation\ValidationException;

class ShiftService
{
    public function openShift(int $userId, float $initialCash): Shift
    {
        $active = Shift::query()
            ->where('user_id', $userId)
            ->whereNull('closed_at')
            ->first();

        if ($active) {
            throw ValidationException::withMessages([
                'shift' => 'Shift already open for this user',
            ]);
        }

        return Shift::query()->create([
            'user_id' => $userId,
            'opened_at' => now(),
            'closed_at' => null,
            'initial_cash' => $initialCash,
            'total_sales' => 0,
        ]);
    }

    public function closeShift(int $userId): Shift
    {
        $shift = Shift::query()
            ->where('user_id', $userId)
            ->whereNull('closed_at')
            ->first();

        if (! $shift) {
            throw ValidationException::withMessages([
                'shift' => 'No active shift found',
            ]);
        }

        $sales = PosTransaction::query()
            ->whereBetween('created_at', [$shift->opened_at, now()])
            ->sum('rounded_total');

        $shift->closed_at = now();
        $shift->total_sales = (float) $sales;
        $shift->save();

        return $shift;
    }
}
