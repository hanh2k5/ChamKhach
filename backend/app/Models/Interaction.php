<?php

namespace App\Models;

use App\Enums\CallResult;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Interaction extends Model
{
    use HasFactory;

    public const UPDATED_AT = null;

    protected $fillable = [
        'customer_id',
        'result',
        'note',
        'contacted_at',
    ];

    protected function casts(): array
    {
        return [
            'result' => CallResult::class,
            'contacted_at' => 'datetime',
        ];
    }

    public function customer(): BelongsTo
    {
        return $this->belongsTo(Customer::class);
    }

    public function needs(): HasMany
    {
        return $this->hasMany(InteractionNeed::class);
    }

    public function followUps(): HasMany
    {
        return $this->hasMany(FollowUp::class);
    }
}
