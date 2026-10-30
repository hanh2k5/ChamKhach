<?php

return [
    /*
    |--------------------------------------------------------------------------
    | Telesales Business Timezone
    |--------------------------------------------------------------------------
    |
    | The business timezone used for converting customer follow-ups and
    | calculating "today" boundary. DB and API timestamps always remain UTC.
    |
    */
    'business_timezone' => env('TELESALES_BUSINESS_TIMEZONE', 'Asia/Ho_Chi_Minh'),

    /*
    |--------------------------------------------------------------------------
    | Pagination Settings
    |--------------------------------------------------------------------------
    |
    | Default pagination limit for API list queries.
    |
    */
    'per_page' => 20,
    'max_per_page' => 50,
];
