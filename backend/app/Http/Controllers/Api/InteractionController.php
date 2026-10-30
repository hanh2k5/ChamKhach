<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreInteractionRequest;
use App\Http\Resources\InteractionResource;
use App\Models\Customer;
use App\Services\InteractionService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Support\Carbon;

class InteractionController extends Controller
{
    public function __construct(
        protected InteractionService $interactionService
    ) {}

    /**
     * GET /api/v1/customers/{customer}/interactions
     * Danh sách interaction, mới nhất trước.
     */
    public function index(Request $request, int $customerId): AnonymousResourceCollection
    {
        $customer = Customer::findOrFail($customerId);

        $perPage = (int) ($request->query('per_page', 20));
        if ($perPage <= 0) {
            $perPage = 20;
        } elseif ($perPage > 100) {
            $perPage = 100;
        }

        $paginator = $this->interactionService->listForCustomer($customer, $perPage);

        return InteractionResource::collection($paginator);
    }

    /**
     * POST /api/v1/customers/{customer}/interactions
     * Ghi nhận cuộc gọi mới (transaction + D6).
     */
    public function store(StoreInteractionRequest $request, int $customerId): JsonResponse
    {
        $customer = Customer::findOrFail($customerId);

        $result = $this->interactionService->record($customer, $request->validated());

        $interaction = $result['interaction'];
        $followUp    = $result['follow_up'];

        return response()->json([
            'interaction'             => [
                'id'           => $interaction->id,
                'result'       => $interaction->result?->value,
                'needs'        => $interaction->needs->pluck('need_type')->map(
                    fn ($n) => $n instanceof \BackedEnum ? $n->value : $n
                )->values()->all(),
                'note'         => $interaction->note,
                'contacted_at' => $interaction->contacted_at?->toIso8601String(),
            ],
            'follow_up'               => $followUp ? [
                'id'          => $followUp->id,
                'customer_id' => $followUp->customer_id,
                'remind_at'   => Carbon::parse($followUp->remind_at)->toIso8601String(),
                'status'      => $followUp->status?->value ?? (string) $followUp->status,
            ] : null,
            'completed_follow_up_ids' => $result['completed_follow_up_ids'],
            'customer_summary'        => $result['customer_summary'],
        ], 201);
    }
}
