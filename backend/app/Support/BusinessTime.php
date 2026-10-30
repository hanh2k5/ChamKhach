<?php

namespace App\Support;

use Carbon\CarbonImmutable;
use Carbon\CarbonInterface;

/**
 * Nguồn sự thật duy nhất cho tính toán thời gian nghiệp vụ.
 *
 * Theo SRS 5.6:
 *  - app.timezone = 'UTC'
 *  - telesales.business_timezone = 'Asia/Ho_Chi_Minh'
 *  - "Hôm nay" = 00:00:00 → 23:59:59.999 theo Asia/Ho_Chi_Minh, đổi sang UTC để query.
 *  - Mọi service nhận $now (CarbonImmutable) hoặc dùng Carbon::setTestNow trong test.
 */
class BusinessTime
{
    /**
     * Múi giờ nghiệp vụ (Asia/Ho_Chi_Minh = UTC+7).
     */
    public static function timezone(): string
    {
        return config('telesales.business_timezone', 'Asia/Ho_Chi_Minh');
    }

    /**
     * Thời điểm "bây giờ" dưới dạng CarbonImmutable (UTC).
     * Dùng CarbonImmutable::now() để tôn trọng Carbon::setTestNow().
     */
    public static function now(): CarbonImmutable
    {
        return CarbonImmutable::now('UTC');
    }

    /**
     * Đầu ngày hôm nay (00:00:00.000) theo Asia/Ho_Chi_Minh, đổi sang UTC.
     */
    public static function todayStartUtc(CarbonImmutable $now): CarbonImmutable
    {
        return $now
            ->setTimezone(static::timezone())
            ->startOfDay()
            ->setTimezone('UTC');
    }

    /**
     * Cuối ngày hôm nay (23:59:59.999999) theo Asia/Ho_Chi_Minh, đổi sang UTC.
     */
    public static function todayEndUtc(CarbonImmutable $now): CarbonImmutable
    {
        return $now
            ->setTimezone(static::timezone())
            ->endOfDay()
            ->setTimezone('UTC');
    }

    /**
     * Xác định nhóm (group) của một follow-up dựa vào remind_at (UTC).
     *
     * D8: overdue  = pending và remind_at < now
     *     today    = pending và now ≤ remind_at ≤ cuối ngày theo Asia/Ho_Chi_Minh
     *     upcoming = pending và remind_at > cuối ngày hôm nay
     */
    public static function followUpGroup(CarbonInterface $remindAt, CarbonImmutable $now): string
    {
        $remindAtUtc  = CarbonImmutable::parse($remindAt)->setTimezone('UTC');
        $todayEnd     = static::todayEndUtc($now);

        if ($remindAtUtc->lt($now)) {
            return 'overdue';
        }

        if ($remindAtUtc->lte($todayEnd)) {
            return 'today';
        }

        return 'upcoming';
    }
}

