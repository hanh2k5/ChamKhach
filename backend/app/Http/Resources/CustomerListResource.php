<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Carbon;

class CustomerListResource extends JsonResource
{
    /**
     * Transform the resource into an array.
     *
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'company_name' => $this->company_name,
            'contact_name' => $this->contact_name,
            'phone' => $this->phone,
            'industry' => $this->industry,
            'potential_level' => $this->potential_level?->value ?? (string) $this->potential_level,
            'status' => $this->status?->value ?? (string) $this->status,
            'interaction_count' => (int) ($this->interaction_count ?? 0),
            'last_contacted_at' => $this->last_contacted_at ? Carbon::parse($this->last_contacted_at)->toIso8601String() : null,
            'last_result' => $this->last_result instanceof \BackedEnum ? $this->last_result->value : $this->last_result,
        ];
    }
}
