<?php

namespace Tests\Unit;

use App\Enums\CallResult;
use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Enums\NeedType;
use App\Enums\PotentialLevel;
use PHPUnit\Framework\TestCase;

class EnumsTest extends TestCase
{
    public function test_call_result_values_and_labels(): void
    {
        $this->assertSame('interested', CallResult::Interested->value);
        $this->assertSame('no_answer', CallResult::NoAnswer->value);
        $this->assertSame('callback', CallResult::Callback->value);
        $this->assertSame('rejected', CallResult::Rejected->value);

        $this->assertSame('Quan tâm', CallResult::Interested->label());
        $this->assertSame('Không nghe máy', CallResult::NoAnswer->label());
        $this->assertSame('Hẹn gọi lại', CallResult::Callback->label());
        $this->assertSame('Từ chối', CallResult::Rejected->label());
    }

    public function test_need_type_values_and_labels(): void
    {
        $this->assertSame('website', NeedType::Website->value);
        $this->assertSame('web_app', NeedType::WebApp->value);
        $this->assertSame('software', NeedType::Software->value);
        $this->assertSame('ads', NeedType::Ads->value);
        $this->assertSame('other', NeedType::Other->value);

        $this->assertSame('Website', NeedType::Website->label());
        $this->assertSame('Web App', NeedType::WebApp->label());
        $this->assertSame('Phần mềm', NeedType::Software->label());
        $this->assertSame('Ads', NeedType::Ads->label());
        $this->assertSame('Chưa xác định/Khác', NeedType::Other->label());
    }

    public function test_potential_level_values_and_labels(): void
    {
        $this->assertSame('high', PotentialLevel::High->value);
        $this->assertSame('medium', PotentialLevel::Medium->value);
        $this->assertSame('low', PotentialLevel::Low->value);
        $this->assertSame('unrated', PotentialLevel::Unrated->value);

        $this->assertSame('Cao', PotentialLevel::High->label());
        $this->assertSame('Vừa', PotentialLevel::Medium->label());
        $this->assertSame('Thấp', PotentialLevel::Low->label());
        $this->assertSame('Chưa đánh giá', PotentialLevel::Unrated->label());
    }

    public function test_customer_status_values_and_labels(): void
    {
        $this->assertSame('active', CustomerStatus::Active->value);
        $this->assertSame('archived', CustomerStatus::Archived->value);

        $this->assertSame('Đang theo dõi', CustomerStatus::Active->label());
        $this->assertSame('Ngừng theo dõi', CustomerStatus::Archived->label());
    }

    public function test_follow_up_status_values(): void
    {
        $this->assertSame('pending', FollowUpStatus::Pending->value);
        $this->assertSame('completed', FollowUpStatus::Completed->value);
        $this->assertSame('cancelled', FollowUpStatus::Cancelled->value);
    }
}
