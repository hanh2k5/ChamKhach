<?php

namespace App\Enums;

enum PotentialLevel: string
{
    case High = 'high';
    case Medium = 'medium';
    case Low = 'low';
    case Unrated = 'unrated';
}
