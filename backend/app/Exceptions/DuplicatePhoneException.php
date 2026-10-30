<?php

namespace App\Exceptions;

use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class DuplicatePhoneException extends Exception
{
    public function __construct(
        public readonly int $existingCustomerId,
        string $message = 'Số điện thoại đã tồn tại.',
    ) {
        parent::__construct($message, 409);
    }

    public function render(Request $request): JsonResponse
    {
        return response()->json([
            'message' => $this->getMessage(),
            'code' => 'DUPLICATE_PHONE',
            'existing_customer_id' => $this->existingCustomerId,
        ], 409);
    }
}
