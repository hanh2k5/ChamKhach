<?php

namespace App\Models;

use App\Enums\NeedType;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class InteractionNeed extends Model
{
    public $timestamps = false;
    public $incrementing = false;

    protected $fillable = [
        'interaction_id',
        'need_type',
    ];

    protected function casts(): array
    {
        return [
            'need_type' => NeedType::class,
        ];
    }

    public function interaction(): BelongsTo
    {
        return $this->belongsTo(Interaction::class);
    }
}
