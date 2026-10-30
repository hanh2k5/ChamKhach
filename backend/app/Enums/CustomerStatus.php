<?php

namespace App\Enums;

enum CustomerStatus: string
{
    case Active = 'active';
    case Archived = 'archived';

    public function label(): string
    {
        return match ($this) {
            self::Active => 'Đang theo dõi',
            self::Archived => 'Ngừng theo dõi',
        };
    }
}
