<?php

use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\ProductController;
use App\Http\Controllers\Api\ShiftController;
use App\Http\Controllers\Api\TransactionController;
use Illuminate\Support\Facades\Route;

Route::post('/login', [AuthController::class, 'login']);
Route::get('/products', [ProductController::class, 'index']);

Route::middleware('api.token')->group(function (): void {
	Route::post('/transaction', [TransactionController::class, 'store']);
	Route::get('/transactions', [TransactionController::class, 'index']);
	Route::get('/stock', [TransactionController::class, 'stock']);
});

Route::middleware('auth:sanctum')->group(function (): void {
    Route::post('/shift/open', [ShiftController::class, 'open']);
    Route::post('/shift/close', [ShiftController::class, 'close']);
});
