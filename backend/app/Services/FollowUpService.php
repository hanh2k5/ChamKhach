<?php

namespace App\Services;

use App\Enums\FollowUpStatus;
use App\Exceptions\FollowUpNotPendingException;
use App\Models\FollowUp;
use App\Support\BusinessTime;
use Carbon\CarbonImmutable;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

class FollowUpService
{
    /**
     * Lấy danh sách follow-up phân trang theo status và group.
     */
    public function list(array $filters, ?CarbonImmutable $now = null): LengthAwarePaginator
    {
        $now = $now ?? BusinessTime::now();
        $todayEnd = BusinessTime::todayEndUtc($now);

        $query = FollowUp::query()->with(['customer', 'interaction']);

        // Lọc theo status: pending (mặc định), completed, cancelled, all
        $status = $filters['status'] ?? FollowUpStatus::Pending->value;
        if ($status !== 'all') {
            $query->where('status', $status);
        }

        // Lọc theo group: overdue, today, upcoming, all (mặc định)
        $group = $filters['group'] ?? 'all';
        if ($group === 'overdue') {
            $query->where('remind_at', '<', $now);
        } elseif ($group === 'today') {
            $query->where('remind_at', '>=', $now)
                ->where('remind_at', '<=', $todayEnd);
        } elseif ($group === 'upcoming') {
            $query->where('remind_at', '>', $todayEnd);
        }

        // Sắp xếp remind_at tăng dần, rồi id tăng dần
        $query->orderBy('remind_at', 'asc')->orderBy('id', 'asc');

        $perPage = (int) ($filters['per_page'] ?? 20);
        if ($perPage <= 0) {
            $perPage = 20;
        } elseif ($perPage > 50) {
            $perPage = 50;
        }

        return $query->paginate($perPage);
    }

    /**
     * Cập nhật trạng thái follow-up (complete, cancel, reschedule).
     *
     * @throws FollowUpNotPendingException
     */
    public function apply(FollowUp $followUp, string $action, ?string $remindAt = null): FollowUp
    {
        return DB::transaction(function () use ($followUp, $action, $remindAt) {
            /** @var FollowUp $locked */
            $locked = FollowUp::where('id', $followUp->id)->lockForUpdate()->firstOrFail();

            if ($locked->status !== FollowUpStatus::Pending) {
                throw new FollowUpNotPendingException();
            }

            if ($action === 'complete') {
                $locked->status = FollowUpStatus::Completed;
                $locked->completed_at = Carbon::now();
            } elseif ($action === 'cancel') {
                $locked->status = FollowUpStatus::Cancelled;
            } elseif ($action === 'reschedule') {
                $locked->status = FollowUpStatus::Pending;
                $locked->remind_at = Carbon::parse($remindAt);
            }

            $locked->save();
            $locked->load(['customer', 'interaction']);

            return $locked;
        });
    }
}
