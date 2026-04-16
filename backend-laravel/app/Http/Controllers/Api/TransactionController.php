<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Services\TransactionService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Validation\ValidationException;
use Throwable;

class TransactionController extends Controller
{
    public function __construct(private readonly TransactionService $service)
    {
    }

    public function store(Request $request): JsonResponse
    {
        if ($request->has('transactions')) {
            return $this->storeBatchPayload($request);
        }

        if ($request->has('transaction') && $request->has('items')) {
            return $this->storeSyncPayload($request);
        }

        return $this->storePosPayload($request);
    }

    // POS Logic
    private function storePosPayload(Request $request): JsonResponse
    {
        $validator = Validator::make($request->all(), [
            'items' => ['required', 'array', 'min:1'],
            'items.*.product_id' => ['required', 'string'],
            'items.*.quantity' => ['required', 'numeric', 'gt:0'],
            'items.*.category' => ['required', 'in:perfume,bottle'],
            'payment_method' => ['required', 'in:cash,qr'],
            'cash_received' => ['nullable', 'numeric', 'min:0'],
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'error' => $validator->errors()->first(),
            ], 400);
        }

        $payload = $validator->validated();
        $items = $payload['items'];

        try {
            $data = $this->service->createPosTransaction($payload);

            return response()->json([
                'success' => true,
                'data' => $data,
            ], 201);
        } catch (ValidationException $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 400);
        } catch (Throwable $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    // Sync Logic
    private function storeSyncPayload(Request $request): JsonResponse
    {
        $validator = Validator::make($request->all(), [
            'transaction' => ['required', 'array'],
            'transaction.total' => ['required', 'numeric', 'min:0'],
            'transaction.localId' => ['nullable', 'numeric'],
            'transaction.createdAt' => ['nullable', 'numeric'],
            'transaction.status' => ['nullable', 'string'],
            'items' => ['required', 'array', 'min:1'],
            'items.*.name' => ['required', 'string'],
            'items.*.type' => ['required', 'string'],
            'items.*.qty' => ['required', 'numeric', 'gt:0'],
            'items.*.subtotal' => ['required', 'numeric', 'min:0'],
            'items.*.product_id' => ['nullable', 'string'],
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'error' => $validator->errors()->first(),
            ], 400);
        }

        $payload = $validator->validated();

        try {
            $data = $this->service->syncSingleTransaction($payload);

            return response()->json([
                'success' => true,
                'data' => $data,
            ], 201);
        } catch (Throwable $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    private function storeBatchPayload(Request $request): JsonResponse
    {
        $validator = Validator::make($request->all(), [
            'transactions' => ['required', 'array', 'min:1'],
            'transactions.*.transaction' => ['required', 'array'],
            'transactions.*.items' => ['required', 'array', 'min:1'],
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'error' => $validator->errors()->first(),
            ], 400);
        }

        $payload = $validator->validated();
        $results = $this->service->syncBatch($payload['transactions']);

        return response()->json([
            'success' => true,
            'data' => $results,
        ], 207);
    }

    public function index(Request $request): JsonResponse
    {
        $limit = (int) $request->query('limit', 50);
        $limit = min(max($limit, 1), 500);

        $transactions = $this->service->listTransactions($limit);

        return response()->json([
            'success' => true,
            'data' => $transactions,
        ]);
    }

    public function stock(): JsonResponse
    {
        $stock = $this->service->stockSnapshot();

        return response()->json([
            'success' => true,
            'data' => $stock,
        ]);
    }
}
