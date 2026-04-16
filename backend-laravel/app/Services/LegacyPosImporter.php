<?php

namespace App\Services;

use App\Models\PosTransaction;
use App\Models\Product;
use App\Models\TransactionItem;
use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use PDO;
use RuntimeException;
use Throwable;

class LegacyPosImporter
{
    public function import(string $legacyDbPath): array
    {
        if (! file_exists($legacyDbPath)) {
            throw new RuntimeException("Legacy DB not found: {$legacyDbPath}");
        }

        $pdo = new PDO('sqlite:'.$legacyDbPath);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        return DB::transaction(function () use ($pdo) {
            $summary = [
                'products_imported' => 0,
                'transactions_imported' => 0,
                'transactions_skipped' => 0,
                'items_imported' => 0,
                'items_skipped' => 0,
            ];

            $productRows = $this->readRows($pdo, 'SELECT * FROM products');
            foreach ($productRows as $productRow) {
                Product::query()->updateOrCreate(
                    ['id' => (string) $productRow['id']],
                    [
                        'name' => (string) $productRow['name'],
                        'category' => (string) $productRow['category'],
                        'price_per_ml' => (float) ($productRow['price_per_ml'] ?? 0),
                        'price' => (float) ($productRow['price'] ?? 0),
                        'capacity_ml' => (int) ($productRow['capacity_ml'] ?? 0),
                        'stock_ml' => (float) ($productRow['stock_ml'] ?? 0),
                        'stock_pcs' => (int) ($productRow['stock_pcs'] ?? 0),
                    ]
                );
                $summary['products_imported']++;
            }

            $transactions = $this->readRows($pdo, 'SELECT * FROM transactions ORDER BY id ASC');
            $legacyToNewTransactionId = [];

            foreach ($transactions as $legacyTransaction) {
                $legacyId = (int) $legacyTransaction['id'];

                $exists = PosTransaction::query()
                    ->where('legacy_id', $legacyId)
                    ->first();

                if ($exists) {
                    $legacyToNewTransactionId[$legacyId] = (int) $exists->id;
                    $summary['transactions_skipped']++;
                    continue;
                }

                $createdAt = $this->safeParseTimestamp((string) ($legacyTransaction['created_at'] ?? ''));

                $newTransaction = PosTransaction::query()->create([
                    'legacy_id' => $legacyId,
                    'items' => (string) ($legacyTransaction['items'] ?? '[]'),
                    'perfume_subtotal' => (float) ($legacyTransaction['perfume_subtotal'] ?? 0),
                    'alcohol_subtotal' => (float) ($legacyTransaction['alcohol_subtotal'] ?? 0),
                    'bottle_subtotal' => (float) ($legacyTransaction['bottle_subtotal'] ?? 0),
                    'total' => (float) ($legacyTransaction['total'] ?? 0),
                    'payment_method' => (string) ($legacyTransaction['payment_method'] ?? 'sync'),
                    'cash_received' => (float) ($legacyTransaction['cash_received'] ?? 0),
                    'change_amount' => (float) ($legacyTransaction['change_amount'] ?? 0),
                    'status' => 'SYNCED',
                    'created_at' => $createdAt,
                    'updated_at' => now(),
                ]);

                $legacyToNewTransactionId[$legacyId] = (int) $newTransaction->id;
                $summary['transactions_imported']++;
            }

            $transactionItemRows = $this->readRows($pdo, 'SELECT * FROM transaction_items ORDER BY id ASC');
            if (count($transactionItemRows) > 0) {
                foreach ($transactionItemRows as $itemRow) {
                    $legacyTransactionId = (int) $itemRow['transaction_id'];
                    $newTransactionId = $legacyToNewTransactionId[$legacyTransactionId] ?? null;

                    if (! $newTransactionId) {
                        $summary['items_skipped']++;
                        continue;
                    }

                    $alreadyExists = TransactionItem::query()
                        ->where('transaction_id', $newTransactionId)
                        ->where('product_id', (string) ($itemRow['product_id'] ?? ''))
                        ->where('product_name', (string) ($itemRow['product_name'] ?? ''))
                        ->where('category', (string) ($itemRow['category'] ?? ''))
                        ->where('quantity', (float) ($itemRow['quantity'] ?? 0))
                        ->where('subtotal', (float) ($itemRow['subtotal'] ?? 0))
                        ->exists();

                    if ($alreadyExists) {
                        $summary['items_skipped']++;
                        continue;
                    }

                    TransactionItem::query()->create([
                        'transaction_id' => $newTransactionId,
                        'product_id' => $itemRow['product_id'] !== null ? (string) $itemRow['product_id'] : null,
                        'product_name' => (string) ($itemRow['product_name'] ?? ''),
                        'category' => (string) ($itemRow['category'] ?? 'unknown'),
                        'quantity' => (float) ($itemRow['quantity'] ?? 0),
                        'unit_price' => (float) ($itemRow['unit_price'] ?? 0),
                        'subtotal' => (float) ($itemRow['subtotal'] ?? 0),
                    ]);
                    $summary['items_imported']++;
                }
            } else {
                foreach ($transactions as $legacyTransaction) {
                    $legacyId = (int) $legacyTransaction['id'];
                    $newTransactionId = $legacyToNewTransactionId[$legacyId] ?? null;
                    if (! $newTransactionId) {
                        continue;
                    }

                    $itemsJson = (string) ($legacyTransaction['items'] ?? '[]');
                    try {
                        $items = json_decode($itemsJson, true, 512, JSON_THROW_ON_ERROR);
                    } catch (Throwable) {
                        $items = [];
                    }

                    foreach ($items as $item) {
                        if (! is_array($item)) {
                            continue;
                        }

                        $quantity = (float) ($item['quantity'] ?? 0);
                        $subtotal = (float) ($item['subtotal'] ?? 0);
                        $unitPrice = $quantity > 0 ? ($subtotal / $quantity) : 0;

                        TransactionItem::query()->create([
                            'transaction_id' => $newTransactionId,
                            'product_id' => isset($item['product_id']) ? (string) $item['product_id'] : null,
                            'product_name' => (string) ($item['product_name'] ?? $item['product_id'] ?? 'legacy-item'),
                            'category' => (string) ($item['category'] ?? 'unknown'),
                            'quantity' => $quantity,
                            'unit_price' => $unitPrice,
                            'subtotal' => $subtotal,
                        ]);
                        $summary['items_imported']++;
                    }
                }
            }

            return $summary;
        });
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function readRows(PDO $pdo, string $sql): array
    {
        $statement = $pdo->query($sql);
        if (! $statement) {
            return [];
        }

        $rows = $statement->fetchAll(PDO::FETCH_ASSOC);
        return is_array($rows) ? $rows : [];
    }

    private function safeParseTimestamp(string $legacyTimestamp): Carbon
    {
        if ($legacyTimestamp === '') {
            return now();
        }

        try {
            return Carbon::parse($legacyTimestamp);
        } catch (Throwable) {
            return now();
        }
    }
}
