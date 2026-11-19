<?php

namespace Tests\Feature;

use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Models\Interaction;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class FollowUpTest extends TestCase
{
    use RefreshDatabase;

    protected User $user;
    protected Customer $customer;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = User::factory()->create(['email' => 'agent@test.local']);
        $this->customer = Customer::create([
            'company_name'     => 'Công ty Alpha',
            'contact_name'     => 'Nguyễn Văn A',
            'phone'            => '0901234567',
            'phone_normalized' => '0901234567',
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
    // Test 6: Mặc định chỉ trả status=pending
    // -----------------------------------------------------------------------
    public function test_list_follow_ups_default_pending_only(): void
    {
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->addDay(),
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->subDay(),
            'status'      => FollowUpStatus::Completed,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->subDay(),
            'status'      => FollowUpStatus::Cancelled,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.status', 'pending');
    }

    // -----------------------------------------------------------------------
    // Test 7: group=overdue khi remind_at < now
    // -----------------------------------------------------------------------
    public function test_group_overdue_when_remind_at_before_now(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 10:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.group', 'overdue');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 8: group=today tại 00:30 VN
    // -----------------------------------------------------------------------
    public function test_group_today_at_vn_00_30(): void
    {
        // 00:00 VN ngày 2026-10-31 = 2026-10-30 17:00:00 UTC
        Carbon::setTestNow(Carbon::parse('2026-10-30 17:00:00', 'UTC'));

        // 00:30 VN ngày 2026-10-31 = 2026-10-30 17:30:00 UTC
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 17:30:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.group', 'today');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 9: group=today tại 23:30 VN
    // -----------------------------------------------------------------------
    public function test_group_today_at_vn_23_30(): void
    {
        // now = 10:00 VN ngày 2026-10-31 = 03:00 UTC
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        // remind_at = 23:30 VN ngày 2026-10-31 = 16:30 UTC
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 16:30:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.group', 'today');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 10: đúng 00:00:00 VN là ranh giới đầu today
    // -----------------------------------------------------------------------
    public function test_group_today_at_vn_midnight_exact(): void
    {
        // now = 00:00:00 VN = 2026-10-30 17:00:00 UTC
        Carbon::setTestNow(Carbon::parse('2026-10-30 17:00:00', 'UTC'));

        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 17:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.group', 'today');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 11: group=upcoming khi remind_at > cuối ngày hôm nay
    // -----------------------------------------------------------------------
    public function test_group_upcoming_when_remind_at_after_today(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        // Cuối ngày VN là 2026-10-31 16:59:59 UTC -> remind_at = 17:00 UTC là sang ngày hôm sau
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 17:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.group', 'upcoming');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 12: lọc group=overdue
    // -----------------------------------------------------------------------
    public function test_filter_by_group_overdue(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 10:00:00', 'UTC'), // overdue
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 05:00:00', 'UTC'), // today
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups?group=overdue');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.group', 'overdue');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 13: lọc group=today
    // -----------------------------------------------------------------------
    public function test_filter_by_group_today(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-30 10:00:00', 'UTC'), // overdue
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 05:00:00', 'UTC'), // today
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups?group=today');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.group', 'today');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 14: lọc group=upcoming
    // -----------------------------------------------------------------------
    public function test_filter_by_group_upcoming(): void
    {
        Carbon::setTestNow(Carbon::parse('2026-10-31 03:00:00', 'UTC'));

        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-10-31 05:00:00', 'UTC'), // today
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-11-01 05:00:00', 'UTC'), // upcoming
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups?group=upcoming');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.group', 'upcoming');

        Carbon::setTestNow();
    }

    // -----------------------------------------------------------------------
    // Test 15: sắp xếp remind_at tăng dần
    // -----------------------------------------------------------------------
    public function test_list_sorted_remind_at_asc(): void
    {
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-11-05 10:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-11-02 10:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::parse('2026-11-03 10:00:00', 'UTC'),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200);
        $remindAts = collect($response->json('data'))->pluck('remind_at')->all();

        $this->assertEquals('2026-11-02T10:00:00+00:00', $remindAts[0]);
        $this->assertEquals('2026-11-03T10:00:00+00:00', $remindAts[1]);
        $this->assertEquals('2026-11-05T10:00:00+00:00', $remindAts[2]);
    }

    // -----------------------------------------------------------------------
    // Test 16: PATCH complete thành công
    // -----------------------------------------------------------------------
    public function test_patch_complete_success(): void
    {
        $followUp = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->addDay(),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action' => 'complete',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('data.status', 'completed');

        $this->assertDatabaseHas('follow_ups', [
            'id'     => $followUp->id,
            'status' => 'completed',
        ]);
    }

    // -----------------------------------------------------------------------
    // Test 17: PATCH cancel thành công
    // -----------------------------------------------------------------------
    public function test_patch_cancel_success(): void
    {
        $followUp = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->addDay(),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action' => 'cancel',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('data.status', 'cancelled');

        $this->assertDatabaseHas('follow_ups', [
            'id'     => $followUp->id,
            'status' => 'cancelled',
        ]);
    }

    // -----------------------------------------------------------------------
    // Test 18: PATCH reschedule thành công
    // -----------------------------------------------------------------------
    public function test_patch_reschedule_success(): void
    {
        $followUp = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->addDay(),
            'status'      => FollowUpStatus::Pending,
        ]);

        $newTime = now()->addDays(3)->startOfHour();

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action'    => 'reschedule',
                'remind_at' => $newTime->toIso8601String(),
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('data.status', 'pending')
            ->assertJsonPath('data.remind_at', $newTime->toIso8601String());

        $this->assertDatabaseHas('follow_ups', [
            'id'     => $followUp->id,
            'status' => 'pending',
        ]);
    }

    // -----------------------------------------------------------------------
    // Test 19: PATCH reschedule với remind_at quá khứ trả 422
    // -----------------------------------------------------------------------
    public function test_patch_reschedule_past_remind_at_returns_422(): void
    {
        $followUp = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->addDay(),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action'    => 'reschedule',
                'remind_at' => '2020-01-01T00:00:00+00:00',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['remind_at']);
    }

    // -----------------------------------------------------------------------
    // Test 20: complete khi đã completed trả 409 FOLLOW_UP_NOT_PENDING
    // -----------------------------------------------------------------------
    public function test_patch_complete_already_completed_returns_409(): void
    {
        $followUp = FollowUp::create([
            'customer_id'  => $this->customer->id,
            'remind_at'    => now()->subDay(),
            'status'       => FollowUpStatus::Completed,
            'completed_at' => now(),
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action' => 'complete',
            ]);

        $response->assertStatus(409)
            ->assertJsonPath('code', 'FOLLOW_UP_NOT_PENDING');
    }

    // -----------------------------------------------------------------------
    // Test 21: cancel khi đã cancelled trả 409
    // -----------------------------------------------------------------------
    public function test_patch_cancel_already_cancelled_returns_409(): void
    {
        $followUp = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->subDay(),
            'status'      => FollowUpStatus::Cancelled,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action' => 'cancel',
            ]);

        $response->assertStatus(409)
            ->assertJsonPath('code', 'FOLLOW_UP_NOT_PENDING');
    }

    // -----------------------------------------------------------------------
    // Test 22: reschedule khi đã completed trả 409
    // -----------------------------------------------------------------------
    public function test_patch_reschedule_already_completed_returns_409(): void
    {
        $followUp = FollowUp::create([
            'customer_id'  => $this->customer->id,
            'remind_at'    => now()->subDay(),
            'status'       => FollowUpStatus::Completed,
            'completed_at' => now(),
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/follow-ups/{$followUp->id}", [
                'action'    => 'reschedule',
                'remind_at' => now()->addDay()->toIso8601String(),
            ]);

        $response->assertStatus(409)
            ->assertJsonPath('code', 'FOLLOW_UP_NOT_PENDING');
    }

    // -----------------------------------------------------------------------
    // Test 23: Trả về note của interaction tạo lịch
    // -----------------------------------------------------------------------
    public function test_list_includes_note_from_interaction(): void
    {
        $interaction = Interaction::create([
            'customer_id'  => $this->customer->id,
            'result'       => \App\Enums\CallResult::Callback,
            'note'         => 'Khách hẹn gọi lại vào sáng mai',
            'contacted_at' => now(),
        ]);

        FollowUp::create([
            'customer_id'    => $this->customer->id,
            'interaction_id' => $interaction->id,
            'remind_at'      => now()->addDay(),
            'status'         => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.note', 'Khách hẹn gọi lại vào sáng mai');
    }

    // -----------------------------------------------------------------------
    // Test 24: Trả về thông tin customer
    // -----------------------------------------------------------------------
    public function test_list_includes_customer_info(): void
    {
        FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => now()->addDay(),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/follow-ups');

        $response->assertStatus(200)
            ->assertJsonPath('data.0.customer.id', $this->customer->id)
            ->assertJsonPath('data.0.customer.company_name', 'Công ty Alpha')
            ->assertJsonPath('data.0.customer.contact_name', 'Nguyễn Văn A')
            ->assertJsonPath('data.0.customer.phone', '0901234567');
    }
}
