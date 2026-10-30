<?php

namespace App\Models;

use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Enums\PotentialLevel;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Customer extends Model
{
    use HasFactory;

    protected $fillable = [
        'company_name',
        'contact_name',
        'phone',
        'phone_normalized',
        'address',
        'industry',
        'source',
        'potential_level',
        'status',
    ];

    protected function casts(): array
    {
        return [
            'potential_level' => PotentialLevel::class,
            'status' => CustomerStatus::class,
        ];
    }

    public function interactions(): HasMany
    {
        return $this->hasMany(Interaction::class);
    }

    public function followUps(): HasMany
    {
        return $this->hasMany(FollowUp::class);
    }

    public function latestInteraction(): HasOne
    {
        return $this->hasOne(Interaction::class)->latestOfMany('contacted_at');
    }

    public function pendingFollowUp(): HasOne
    {
        return $this->hasOne(FollowUp::class)->where('status', FollowUpStatus::Pending->value);
    }
}
