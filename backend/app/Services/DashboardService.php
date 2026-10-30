<?php

namespace App\Services;

use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Enums\PotentialLevel;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Support\BusinessTime;
use Carbon\CarbonImmutable;
use Illuminate\Database\Eloquent\Collection;

class DashboardService
{
    /**
     * Lấy dữ liệu tổng quan dashboard.
     *
     * @return array{
     *   overdue_count: int,
     *   today_count: int,
     *   potential_count: int,
     *   items: Collection<int, FollowUp>
     * }
     */
    public function getSummary(?CarbonImmutable $now = null): array
    {
        $now = $now ?? BusinessTime::now();
        $todayEnd = BusinessTime::todayEndUtc($now);

        $overdueCount = FollowUp::where('status', FollowUpStatus::Pending)
            ->where('remind_at', '<', $now)
            ->count();

        $todayCount = FollowUp::where('status', FollowUpStatus::Pending)
            ->where('remind_at', '>=', $now)
            ->where('remind_at', '<=', $todayEnd)
            ->count();

        $potentialCount = Customer::where('status', CustomerStatus::Active)
            ->whereIn('potential_level', [
                PotentialLevel::High->value,
                PotentialLevel::Medium->value,
            ])
            ->count();

        $items = FollowUp::where('status', FollowUpStatus::Pending)
            ->with(['customer', 'interaction'])
            ->orderBy('remind_at', 'asc')
            ->orderBy('id', 'asc')
            ->limit(20)
            ->get();

        return [
            'overdue_count'   => $overdueCount,
            'today_count'     => $todayCount,
            'potential_count' => $potentialCount,
            'items'           => $items,
        ];
    }
}
