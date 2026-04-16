<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Services\ShiftService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Throwable;

class ShiftController extends Controller
{
    public function __construct(private readonly ShiftService $service)
    {
    }

    public function open(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'initial_cash' => ['required', 'numeric', 'min:0'],
        ]);

        try {
            $shift = $this->service->openShift((int) $request->user()->id, (float) $validated['initial_cash']);

            return response()->json([
                'success' => true,
                'data' => $shift,
            ], 201);
        } catch (ValidationException $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 422);
        } catch (Throwable $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    public function close(Request $request): JsonResponse
    {
        try {
            $shift = $this->service->closeShift((int) $request->user()->id);

            return response()->json([
                'success' => true,
                'data' => $shift,
            ]);
        } catch (ValidationException $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 422);
        } catch (Throwable $e) {
            return response()->json([
                'success' => false,
                'error' => $e->getMessage(),
            ], 500);
        }
    }
}
