<?php

namespace App\Http\Resources;

use App\Support\BusinessTime;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Carbon;

class FollowUpResource extends JsonResource
{
    public function toArray(Request $request): array
    {
        $group = $this->group ?? ($this->remind_at ? BusinessTime::followUpGroup($this->remind_at, BusinessTime::now()) : null);

        $customerData = null;
        if ($this->relationLoaded('customer') && $this->customer) {
            $customerData = [
                'id' => $this->customer->id,
                'company_name' => $this->customer->company_name,
                'contact_name' => $this->customer->contact_name,
                'phone' => $this->customer->phone,
            ];
        }

        $note = null;
        if ($this->relationLoaded('interaction')) {
            $note = $this->interaction?->note;
        }

        return [
            'id' => $this->id,
            'remind_at' => $this->remind_at ? Carbon::parse($this->remind_at)->toIso8601String() : null,
            'status' => $this->status instanceof \BackedEnum ? $this->status->value : (string) $this->status,
            'group' => $group,
            'customer' => $customerData,
            'note' => $note,
        ];
    }
}
