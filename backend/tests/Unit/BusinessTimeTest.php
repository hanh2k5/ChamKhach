<?php

namespace Tests\Unit;

use App\Support\BusinessTime;
use Carbon\CarbonImmutable;
use Tests\TestCase;

/**
 * Unit test cho BusinessTime.
 *
 * Theo SRS 5.6 và mục 11:
 * - "Hôm nay" = 00:00 đến 23:59:59.999 theo Asia/Ho_Chi_Minh, đổi sang UTC để query.
 * - Asia/Ho_Chi_Minh = UTC+7, nên:
 *     00:00:00 VN = 17:00:00 UTC ngày trước
 *     23:59:59 VN = 16:59:59 UTC cùng ngày
 */
class BusinessTimeTest extends TestCase
{
    // -----------------------------------------------------------------------
    // Test 1: todayStartUtc tại 00:00:00 VN = 17:00:00 UTC ngày trước
    // -----------------------------------------------------------------------
    public function test_today_start_utc_at_vn_midnight(): void
    {
        // "Bây giờ" là 00:30:00 VN ngày 2026-10-31 = 17:30:00 UTC 2026-10-30
        $now = CarbonImmutable::parse('2026-10-30 17:30:00', 'UTC');

        $start = BusinessTime::todayStartUtc($now);

        // 00:00:00 VN 2026-10-31 = 17:00:00 UTC 2026-10-30
        $this->assertEquals('2026-10-30 17:00:00', $start->format('Y-m-d H:i:s'));
    }

    // -----------------------------------------------------------------------
    // Test 2: todayEndUtc tại 23:59:59.999 VN = 16:59:59 UTC cùng ngày
    // -----------------------------------------------------------------------
    public function test_today_end_utc_at_vn_end_of_day(): void
    {
        // "Bây giờ" là 10:00:00 VN ngày 2026-10-31 = 03:00:00 UTC 2026-10-31
        $now = CarbonImmutable::parse('2026-10-31 03:00:00', 'UTC');

        $end = BusinessTime::todayEndUtc($now);

        // 23:59:59 VN 2026-10-31 = 16:59:59 UTC 2026-10-31
        $this->assertEquals('2026-10-31 16:59:59', $end->format('Y-m-d H:i:s'));
    }

    // -----------------------------------------------------------------------
    // Test 3: 00:30 VN → group today
    // -----------------------------------------------------------------------
    public function test_group_today_at_vn_00_30(): void
    {
        // now = 00:00:00 VN = 17:00:00 UTC ngày trước
        $now = CarbonImmutable::parse('2026-10-30 17:00:00', 'UTC');

        // remind_at = 00:30:00 VN = 17:30:00 UTC ngày trước (cùng ngày VN)
        $remindAt = CarbonImmutable::parse('2026-10-30 17:30:00', 'UTC');

        $group = BusinessTime::followUpGroup($remindAt, $now);

        $this->assertEquals('today', $group);
    }

    // -----------------------------------------------------------------------
    // Test 4: 23:30 VN → group today
    // -----------------------------------------------------------------------
    public function test_group_today_at_vn_23_30(): void
    {
        // now = 10:00:00 VN = 03:00:00 UTC
        $now = CarbonImmutable::parse('2026-10-31 03:00:00', 'UTC');

        // remind_at = 23:30:00 VN = 16:30:00 UTC (cùng ngày, trước 16:59:59 UTC = cuối ngày VN)
        $remindAt = CarbonImmutable::parse('2026-10-31 16:30:00', 'UTC');

        $group = BusinessTime::followUpGroup($remindAt, $now);

        $this->assertEquals('today', $group);
    }

    // -----------------------------------------------------------------------
    // Test 5: đúng 00:00:00 VN = đầu today (ranh giới trái)
    // -----------------------------------------------------------------------
    public function test_boundary_exactly_midnight_vn_is_start_of_today(): void
    {
        // now = đúng 00:00:00 VN = 17:00:00 UTC ngày trước
        $now = CarbonImmutable::parse('2026-10-30 17:00:00', 'UTC');

        // remind_at = đúng 00:00:00 VN = 17:00:00 UTC (= now, không phải quá khứ)
        $remindAt = CarbonImmutable::parse('2026-10-30 17:00:00', 'UTC');

        $group = BusinessTime::followUpGroup($remindAt, $now);

        // remind_at = now, không lt(now) → today
        $this->assertEquals('today', $group);
    }

    // -----------------------------------------------------------------------
    // Test thêm: overdue khi remind_at < now
    // -----------------------------------------------------------------------
    public function test_group_overdue_when_remind_at_before_now(): void
    {
        $now      = CarbonImmutable::parse('2026-10-31 03:00:00', 'UTC');
        $remindAt = CarbonImmutable::parse('2026-10-30 10:00:00', 'UTC');

        $group = BusinessTime::followUpGroup($remindAt, $now);

        $this->assertEquals('overdue', $group);
    }

    // -----------------------------------------------------------------------
    // Test thêm: upcoming khi remind_at > cuối ngày VN
    // -----------------------------------------------------------------------
    public function test_group_upcoming_when_remind_at_after_today(): void
    {
        // now = 10:00:00 VN = 03:00:00 UTC
        $now = CarbonImmutable::parse('2026-10-31 03:00:00', 'UTC');

        // remind_at = 00:00:00 VN ngày mai = 17:00:00 UTC hôm nay (sau 16:59:59 UTC)
        $remindAt = CarbonImmutable::parse('2026-10-31 17:00:00', 'UTC');

        $group = BusinessTime::followUpGroup($remindAt, $now);

        $this->assertEquals('upcoming', $group);
    }
}

