<?php

namespace App\Enums;

enum NeedType: string
{
    case Website = 'website';
    case WebApp = 'web_app';
    case Software = 'software';
    case Ads = 'ads';
    case Other = 'other';

    public function label(): string
    {
        return match ($this) {
            self::Website => 'Website',
            self::WebApp => 'Web App',
            self::Software => 'Phần mềm',
            self::Ads => 'Ads',
            self::Other => 'Chưa xác định/Khác',
        };
    }
}
