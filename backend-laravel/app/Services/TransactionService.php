<?php

namespace App\Services;

use App\Models\PosTransaction;
use App\Models\Product;
use App\Models\TransactionItem;
use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

class TransactionService
{
    private const ALCOHOL_PRICE_PER_ML = 2000;
    private const ALCOHOL_FREE_THRESHOLD_ML = 100;

    public function createPosTransaction(array $payload): array
    {
        $items = $payload['items'];
        $paymentMethod = (string) $payload['payment_method'];
        $cashReceivedInput = (float) ($payload['cash_received'] ?? 0);

        return DB::transaction(function () use ($items, $paymentMethod, $cashReceivedInput) {
            $perfumeSubtotal = 0.0;
            $bottleSubtotal = 0.0;
            $totalBibitMl = 0.0;
            $insertedItems = [];
            $updatedStock = [];

            foreach ($items as $item) {
                $product = Product::query()
                    ->where('id', $item['product_id'])
                    ->lockForUpdate()
                    ->first();

                if (! $product) {
                    throw ValidationException::withMessages([
                        'items' => "Product {$item['product_id']} not found",
                    ]);
                }

                $quantity = (float) $item['quantity'];
                $category = (string) $item['category'];

                if ($category === 'perfume') {
                    if ($quantity > (float) $product->stock_ml) {
                        throw ValidationException::withMessages([
                            'items' => "Insufficient stock for {$product->name}. Available: {$product->stock_ml} ml",
                        ]);
                    }

                    $subtotal = $quantity * (float) $product->price_per_ml;
                    $perfumeSubtotal += $subtotal;
                    $totalBibitMl += $quantity;
                    $product->stock_ml = (float) $product->stock_ml - $quantity;
                    $product->save();

                    $insertedItems[] = [
                        'product_id' => $product->id,
                        'product_name' => $product->name,
                        'category' => $category,
                        'quantity' => $quantity,
                        'subtotal' => $subtotal,
                        'unit_price' => (float) $product->price_per_ml,
                    ];
                } elseif ($category === 'bottle') {
                    if ($quantity > (float) $product->stock_pcs) {
                        throw ValidationException::withMessages([
                            'items' => "Insufficient stock for {$product->name}. Available: {$product->stock_pcs} pcs",
                        ]);
                    }

                    $subtotal = $quantity * (float) $product->price;
                    $bottleSubtotal += $subtotal;
                    $product->stock_pcs = (int) ((float) $product->stock_pcs - $quantity);
                    $product->save();

                    $insertedItems[] = [
                        'product_id' => $product->id,
                        'product_name' => $product->name,
                        'category' => $category,
                        'quantity' => $quantity,
                        'subtotal' => $subtotal,
                        'unit_price' => (float) $product->price,
                    ];
                }

                $updatedStock[] = [
                    'product_id' => $product->id,
                    'product_name' => $product->name,
                    'stock_ml' => (float) $product->stock_ml,
                    'stock_pcs' => (int) $product->stock_pcs,
                ];
            }

            $alcoholSubtotal = $totalBibitMl < self::ALCOHOL_FREE_THRESHOLD_ML
                ? 0.0
                : $totalBibitMl * self::ALCOHOL_PRICE_PER_ML;

            $total = $perfumeSubtotal + $alcoholSubtotal + $bottleSubtotal;
            $roundedTotal = $this->roundToHundred($total);

            $cashReceived = 0.0;
            $changeAmount = 0.0;
            if ($paymentMethod === 'cash') {
                $cashReceived = $cashReceivedInput;
                if ($cashReceived < $roundedTotal) {
                    throw ValidationException::withMessages([
                        'cash_received' => 'Insufficient payment.',
                    ]);
                }
                $changeAmount = $cashReceived - $roundedTotal;
            }

            $transaction = PosTransaction::query()->create([
                'items' => json_encode($items, JSON_UNESCAPED_UNICODE),
                'perfume_subtotal' => $perfumeSubtotal,
                'alcohol_subtotal' => $alcoholSubtotal,
                'bottle_subtotal' => $bottleSubtotal,
                'total' => $total,
                'rounded_total' => $roundedTotal,
                'payment_method' => $paymentMethod,
                'cash_received' => $cashReceived,
                'change_amount' => $changeAmount,
                'status' => 'SYNCED',
            ]);

            foreach ($insertedItems as $insertedItem) {
                TransactionItem::query()->create([
                    'transaction_id' => $transaction->id,
                    'product_id' => $insertedItem['product_id'],
                    'product_name' => $insertedItem['product_name'],
                    'category' => $insertedItem['category'],
                    'quantity' => $insertedItem['quantity'],
                    'unit_price' => $insertedItem['unit_price'],
                    'subtotal' => $insertedItem['subtotal'],
                ]);
            }

            return [
                'transaction_id' => $transaction->id,
                'items' => $insertedItems,
                'summary' => [
                    'perfume_subtotal' => $perfumeSubtotal,
                    'alcohol_ml' => $totalBibitMl,
                    'alcohol_subtotal' => $alcoholSubtotal,
                    'bottle_subtotal' => $bottleSubtotal,
                    'total' => $total,
                    'rounded_total' => $roundedTotal,
                    'payment_method' => $paymentMethod,
                    'cash_received' => $paymentMethod === 'cash' ? $cashReceived : null,
                    'change' => $paymentMethod === 'cash' ? $changeAmount : null,
                ],
                'updated_stock' => $updatedStock,
            ];
        });
    }

    public function syncSingleTransaction(array $payload): array
    {
        return DB::transaction(function () use ($payload) {
            $transactionPayload = $payload['transaction'];
            $items = $payload['items'];

            $perfumeSubtotal = 0.0;
            $bottleSubtotal = 0.0;
            foreach ($items as $item) {
                $type = strtolower((string) $item['type']);
                if ($type === 'perfume') {
                    $perfumeSubtotal += (float) $item['subtotal'];
                }
                if ($type === 'bottle') {
                    $bottleSubtotal += (float) $item['subtotal'];
                }
            }

            $total = (float) $transactionPayload['total'];
            $roundedTotal = isset($transactionPayload['roundedTotal'])
                ? (float) $transactionPayload['roundedTotal']
                : $this->roundToHundred($total);
            $alcoholSubtotal = max(0, $total - ($perfumeSubtotal + $bottleSubtotal));

            $status = strtoupper((string) ($transactionPayload['status'] ?? 'PENDING'));
            if (! in_array($status, ['PENDING', 'SYNCED'], true)) {
                $status = 'PENDING';
            }

            $createdAt = now();
            if (! empty($transactionPayload['createdAt'])) {
                $createdAt = Carbon::createFromTimestampMs((int) $transactionPayload['createdAt']);
            }

            $transaction = PosTransaction::query()->create([
                'items' => json_encode($items, JSON_UNESCAPED_UNICODE),
                'perfume_subtotal' => $perfumeSubtotal,
                'alcohol_subtotal' => $alcoholSubtotal,
                'bottle_subtotal' => $bottleSubtotal,
                'total' => $total,
                'rounded_total' => $roundedTotal,
                'payment_method' => 'sync',
                'cash_received' => $roundedTotal,
                'change_amount' => 0,
                'status' => $status,
                'created_at' => $createdAt,
                'updated_at' => now(),
            ]);

            foreach ($items as $item) {
                $qty = (float) $item['qty'];
                $subtotal = (float) $item['subtotal'];

                TransactionItem::query()->create([
                    'transaction_id' => $transaction->id,
                    'product_id' => $item['product_id'] ?? null,
                    'product_name' => (string) $item['name'],
                    'category' => strtolower((string) $item['type']),
                    'quantity' => $qty,
                    'unit_price' => $qty > 0 ? ($subtotal / $qty) : 0,
                    'subtotal' => $subtotal,
                ]);
            }

            return [
                'transaction_id' => $transaction->id,
                'summary' => [
                    'perfume_subtotal' => $perfumeSubtotal,
                    'alcohol_subtotal' => $alcoholSubtotal,
                    'bottle_subtotal' => $bottleSubtotal,
                    'total' => $total,
                    'rounded_total' => $roundedTotal,
                ],
            ];
        });
    }

    public function syncBatch(array $batchPayload): array
    {
        $results = [];

        foreach ($batchPayload as $entry) {
            $localId = $entry['transaction']['localId'] ?? null;
            try {
                $data = $this->syncSingleTransaction($entry);
                $results[] = [
                    'local_id' => $localId,
                    'success' => true,
                    'data' => $data,
                ];
            } catch (\Throwable $e) {
                $results[] = [
                    'local_id' => $localId,
                    'success' => false,
                    'error' => $e->getMessage(),
                ];
            }
        }

        return $results;
    }

    public function listTransactions(int $limit): array
    {
        return PosTransaction::query()
            ->orderByDesc('created_at')
            ->limit($limit)
            ->get()
            ->map(function (PosTransaction $transaction) {
                $row = $transaction->toArray();
                $row['items'] = json_decode((string) $transaction->items, true) ?? [];
                return $row;
            })
            ->values()
            ->all();
    }

    public function stockSnapshot(): array
    {
        return Product::query()
            ->select(['id', 'name', 'category', 'stock_ml', 'stock_pcs'])
            ->orderBy('category')
            ->orderBy('name')
            ->get()
            ->all();
    }

    private function roundToHundred(float $value): float
    {
        return round($value / 100) * 100;
    }
}
