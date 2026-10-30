<?php

namespace App\Enums;

enum CallResult: string
{
    case Interested = 'interested';
    case NoAnswer = 'no_answer';
    case Callback = 'callback';
    case Rejected = 'rejected';

    public function label(): string
    {
        return match ($this) {
            self::Interested => 'Quan tâm',
            self::NoAnswer => 'Không nghe máy',
            self::Callback => 'Hẹn gọi lại',
            self::Rejected => 'Từ chối',
        };
    }
}
