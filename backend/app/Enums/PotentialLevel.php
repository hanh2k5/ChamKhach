<?php

namespace App\Enums;

enum PotentialLevel: string
{
    case High = 'high';
    case Medium = 'medium';
    case Low = 'low';
    case Unrated = 'unrated';

    public function label(): string
    {
        return match ($this) {
            self::High => 'Cao',
            self::Medium => 'Vừa',
            self::Low => 'Thấp',
            self::Unrated => 'Chưa đánh giá',
        };
    }
}
