<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Carbon;

class CustomerResource extends JsonResource
{
    /**
     * Optional additional data attached dynamically (e.g. cancelled_follow_up_ids).
     */
    protected array $extraData = [];

    public function withExtra(array $extra): self
    {
        $this->extraData = $extra;
        return $this;
    }

    /**
     * Transform the resource into an array.
     *
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $nextFollowUp = null;
        if ($this->relationLoaded('followUps')) {
            $pending = $this->followUps
                ->where('status', \App\Enums\FollowUpStatus::Pending)
                ->sortBy('remind_at')
                ->first();

            if ($pending) {
                $nextFollowUp = [
                    'id' => $pending->id,
                    'remind_at' => Carbon::parse($pending->remind_at)->toIso8601String(),
                ];
            }
        } elseif (isset($this->next_follow_up)) {
            $nextFollowUp = $this->next_follow_up;
        }

        $data = [
            'id' => $this->id,
            'company_name' => $this->company_name,
            'contact_name' => $this->contact_name,
            'phone' => $this->phone,
            'phone_normalized' => $this->phone_normalized,
            'address' => $this->address,
            'industry' => $this->industry,
            'source' => $this->source,
            'potential_level' => $this->potential_level?->value ?? (string) $this->potential_level,
            'status' => $this->status?->value ?? (string) $this->status,
            'interaction_count' => (int) ($this->interaction_count ?? 0),
            'last_contacted_at' => $this->last_contacted_at ? Carbon::parse($this->last_contacted_at)->toIso8601String() : null,
            'last_result' => $this->last_result instanceof \BackedEnum ? $this->last_result->value : $this->last_result,
            'next_follow_up' => $nextFollowUp,
            'created_at' => $this->created_at?->toIso8601String(),
            'updated_at' => $this->updated_at?->toIso8601String(),
        ];

        if (! empty($this->extraData)) {
            $data = array_merge($data, $this->extraData);
        }

        return $data;
    }
}
