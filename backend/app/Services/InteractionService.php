<?php

namespace App\Services;

use App\Enums\CallResult;
use App\Enums\FollowUpStatus;
use App\Enums\NeedType;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Models\Interaction;
use App\Models\InteractionNeed;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

class InteractionService
{
    /**
     * Ghi nhận một cuộc gọi mới (transaction theo SRS mục 8).
     *
     * @param  Customer $customer
     * @param  array{
     *   result: string,
     *   needs: string[],
     *   note: string|null,
     *   follow_up: array{remind_at: string}|null
     * } $dto
     * @return array{
     *   interaction: Interaction,
     *   completed_follow_up_ids: int[],
     *   follow_up: FollowUp|null,
     *   customer_summary: array{interaction_count: int, last_contacted_at: string|null, last_result: string|null}
     * }
     */
    public function record(Customer $customer, array $dto): array
    {
        return DB::transaction(function () use ($customer, $dto) {
            // Bước 2: Khoá hàng để tuần tự hoá các thao tác của khách hàng.
            // lockForUpdate() trả Builder → cần fresh() thay vì chain refresh()
            $customer = Customer::where('id', $customer->id)->lockForUpdate()->firstOrFail();

            // Bước 3: Tạo interaction
            $interaction = Interaction::create([
                'customer_id'  => $customer->id,
                'result'       => CallResult::from($dto['result']),
                'note'         => $dto['note'] ?? null,
                'contacted_at' => Carbon::now(),
            ]);

            // Tạo interaction_needs
            foreach ($dto['needs'] as $needValue) {
                InteractionNeed::create([
                    'interaction_id' => $interaction->id,
                    'need_type'      => NeedType::from($needValue),
                ]);
            }

            // Bước 4: Hoàn thành follow-up pending cũ (quy tắc D6)
            $pendingFollowUps = FollowUp::where('customer_id', $customer->id)
                ->where('status', FollowUpStatus::Pending)
                ->lockForUpdate()
                ->get();

            $completedFollowUpIds = $pendingFollowUps->pluck('id')->all();

            if (! empty($completedFollowUpIds)) {
                FollowUp::whereIn('id', $completedFollowUpIds)->update([
                    'status'       => FollowUpStatus::Completed,
                    'completed_at' => Carbon::now(),
                ]);
            }

            // Bước 5: Tạo follow-up mới nếu có remind_at
            $newFollowUp = null;
            if (! empty($dto['follow_up']['remind_at'])) {
                $newFollowUp = FollowUp::create([
                    'customer_id'    => $customer->id,
                    'interaction_id' => $interaction->id,
                    'remind_at'      => Carbon::parse($dto['follow_up']['remind_at']),
                    'status'         => FollowUpStatus::Pending,
                ]);
            }

            // Tính customer_summary sau transaction
            $interactionCount   = Interaction::where('customer_id', $customer->id)->count();
            $lastInteraction    = Interaction::where('customer_id', $customer->id)
                ->orderByDesc('contacted_at')
                ->orderByDesc('id')
                ->first();
            $lastContactedAt    = $lastInteraction?->contacted_at?->toIso8601String();
            $lastResult         = $lastInteraction?->result?->value;

            // Load needs cho response
            $interaction->load('needs');

            return [
                'interaction'             => $interaction,
                'completed_follow_up_ids' => $completedFollowUpIds,
                'follow_up'               => $newFollowUp,
                'customer_summary'        => [
                    'interaction_count'  => $interactionCount,
                    'last_contacted_at'  => $lastContactedAt,
                    'last_result'        => $lastResult,
                ],
            ];
        });
    }

    /**
     * Lấy danh sách interaction của một khách, mới nhất trước.
     */
    public function listForCustomer(Customer $customer, int $perPage): \Illuminate\Contracts\Pagination\LengthAwarePaginator
    {
        return Interaction::where('customer_id', $customer->id)
            ->with('needs')
            ->orderByDesc('contacted_at')
            ->orderByDesc('id')
            ->paginate($perPage);
    }
}
