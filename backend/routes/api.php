<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->middleware('auth:sanctum')->group(function () {
    Route::get('/ping', function (Request $request) {
        return response()->json([
            'status' => 'ok',
            'message' => 'pong',
            'user' => [
                'id' => $request->user()->id,
                'email' => $request->user()->email,
            ],
        ]);
    });

    Route::apiResource('customers', \App\Http\Controllers\Api\CustomerController::class)
        ->only(['index', 'store', 'show', 'update']);
});
