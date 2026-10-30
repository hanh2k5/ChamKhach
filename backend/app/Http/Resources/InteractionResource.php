<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Carbon;

/**
 * Resource cho một bản ghi interaction (dùng trong GET list).
 */
class InteractionResource extends JsonResource
{
    public function toArray(Request $request): array
    {
        return [
            'id'           => $this->id,
            'result'       => $this->result?->value ?? (string) $this->result,
            'needs'        => $this->whenLoaded('needs', fn () =>
                $this->needs->pluck('need_type')->map(fn ($n) =>
                    $n instanceof \BackedEnum ? $n->value : $n
                )->values()->all()
            ),
            'note'         => $this->note,
            'contacted_at' => $this->contacted_at
                ? Carbon::parse($this->contacted_at)->toIso8601String()
                : null,
        ];
    }
}
