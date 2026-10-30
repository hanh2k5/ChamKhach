<?php

namespace App\Exceptions;

use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class FollowUpNotPendingException extends Exception
{
    public function __construct(
        string $message = 'Lịch hẹn không ở trạng thái chờ xử lý.',
    ) {
        parent::__construct($message, 409);
    }

    public function render(Request $request): JsonResponse
    {
        return response()->json([
            'message' => $this->getMessage(),
            'code' => 'FOLLOW_UP_NOT_PENDING',
        ], 409);
    }
}
