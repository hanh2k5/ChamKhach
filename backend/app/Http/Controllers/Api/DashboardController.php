<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Resources\FollowUpResource;
use App\Services\DashboardService;
use Illuminate\Http\JsonResponse;

class DashboardController extends Controller
{
    public function __construct(
        protected DashboardService $dashboardService
    ) {}

    /**
     * GET /api/v1/dashboard
     * Dữ liệu tổng quan dashboard gồm các chỉ số lịch gọi và tối đa 20 items.
     */
    public function index(): JsonResponse
    {
        $summary = $this->dashboardService->getSummary();

        return response()->json([
            'overdue_count'   => $summary['overdue_count'],
            'today_count'     => $summary['today_count'],
            'potential_count' => $summary['potential_count'],
            'items'           => FollowUpResource::collection($summary['items']),
        ]);
    }
}
