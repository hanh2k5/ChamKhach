<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\PatchFollowUpRequest;
use App\Http\Resources\FollowUpResource;
use App\Models\FollowUp;
use App\Services\FollowUpService;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

class FollowUpController extends Controller
{
    public function __construct(
        protected FollowUpService $followUpService
    ) {}

    /**
     * GET /api/v1/follow-ups
     * Danh sách follow-up phân trang theo status và group.
     */
    public function index(Request $request): AnonymousResourceCollection
    {
        $paginator = $this->followUpService->list($request->all());

        return FollowUpResource::collection($paginator);
    }

    /**
     * PATCH /api/v1/follow-ups/{id}
     * Cập nhật follow-up (complete, cancel, reschedule).
     */
    public function update(PatchFollowUpRequest $request, int $id): FollowUpResource
    {
        $followUp = FollowUp::findOrFail($id);

        $updated = $this->followUpService->apply(
            $followUp,
            $request->input('action'),
            $request->input('remind_at')
        );

        return new FollowUpResource($updated);
    }
}
