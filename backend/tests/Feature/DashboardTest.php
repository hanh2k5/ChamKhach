<?php

namespace Tests\Feature;

use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class DashboardTest extends TestCase
{
    use RefreshDatabase;

    protected User $user;
    protected Customer $customer;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = User::factory()->create(['email' => 'agent@test.local']);
        $this->customer = Customer::create([
            'company_name'     => 'Công ty Beta',
            'contact_name'     => 'Trần Văn B',
            'phone'            => '0987654321',
            'phone_normalized' => '0987654321',
            'status'           => CustomerStatus::Active,
        ]);
    }

    protected function authHeaders(): array
    {
        $token = $this->user->createToken('test')->plainTextToken;
        return [
            'Authorization' => 'Bearer ' . $token,
            'Accept'        => 'application/json',
        ];
    }

    // -----------------------------------------------------------------------
    // Test 25: Đếm đúng overdue_count
    // -----------------------------------------------------------------------
    public function test_dashboard_counts_overdue(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        // 2 overdue, 1 today
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 01:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 05:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 05:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/dashboard');

        $response->assertStatus(200)
            ->assertJsonPath('overdue_count', 2);

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 26: Đếm đúng today_count
    // -----------------------------------------------------------------------
    public function test_dashboard_counts_today(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 05:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 08:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/dashboard');

        $response->assertStatus(200)
            ->assertJsonPath('today_count', 2);

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 28: Items tối đa 20 bản ghi
    // -----------------------------------------------------------------------
    public function test_dashboard_items_max_20(): void
    {
        for ($i = 1; $i <= 25; $i++) {
            FollowUp::create([
                'customer_id' => $this->customer->id,
                'remind_at'   => now()->addDays($i),
                'status'      => FollowUpStatus::Pending,
            ]);
        }

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/dashboard');

        $response->assertStatus(200)
            ->assertJsonCount(20, 'items');
    }

    // -----------------------------------------------------------------------
    // Test 29: Thứ tự items: quá hạn trước, sau đó theo remind_at
    // -----------------------------------------------------------------------
    public function test_dashboard_items_order_overdue_first(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        $f1 = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-11-01 03:00:00', 'UTC'), // upcoming
            'status'      => FollowUpStatus::Pending,
        ]);

        $f2 = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 01:00:00', 'UTC'), // overdue
            'status'      => FollowUpStatus::Pending,
        ]);

        $f3 = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 05:00:00', 'UTC'), // today
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/dashboard');

        $response->assertStatus(200);
        $ids = collect($response->json('items'))->pluck('id')->all();

        // Thứ tự mong đợi: overdue ($f2), today ($f3), upcoming ($f1)
        $this->assertEquals([$f2->id, $f3->id, $f1->id], $ids);

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 30: Không có data -> các count = 0, items = []
    // -----------------------------------------------------------------------
    public function test_dashboard_counts_all_zero_when_empty(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/dashboard');

        $response->assertStatus(200)
            ->assertExactJson([
                'overdue_count'   => 0,
                'today_count'     => 0,
                'potential_count' => 0,
                'items'           => [],
            ]);
    }
}
