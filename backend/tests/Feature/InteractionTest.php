<?php

namespace Tests\Feature;

use App\Enums\CallResult;
use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Enums\NeedType;
use App\Enums\PotentialLevel;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Models\Interaction;
use App\Models\InteractionNeed;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

class InteractionTest extends TestCase
{
    use RefreshDatabase;

    protected User $user;
    protected Customer $customer;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = User::factory()->create(['email' => 'agent@test.local']);
        $this->customer = Customer::create([
            'company_name'     => 'Công ty Test',
            'phone'            => '0901234567',
            'phone_normalized' => '0901234567',
            'potential_level'  => PotentialLevel::Medium,
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
    // Test 1: interaction_count tăng 1 sau khi tạo
    // -----------------------------------------------------------------------
    public function test_create_interaction_increases_count_by_one(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'callback',
                'needs'        => ['website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('customer_summary.interaction_count', 1);

        $this->assertDatabaseCount('interactions', 1);
    }

    public function test_create_interaction_assigns_contacted_at_on_server(): void
    {
        $contactedAt = Carbon::parse('2026-11-02T12:34:56+00:00');
        Carbon::setTestNow($contactedAt);

        try {
            $response = $this->withHeaders($this->authHeaders())
                ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                    'result' => 'callback',
                    'needs' => ['website'],
                ]);

            $response->assertCreated()
                ->assertJsonPath('interaction.contacted_at', $contactedAt->toIso8601String());
            $this->assertDatabaseHas('interactions', [
                'customer_id' => $this->customer->id,
                'contacted_at' => $contactedAt->toDateTimeString(),
            ]);
        } finally {
            Carbon::setTestNow();
        }
    }

    // -----------------------------------------------------------------------
    // Test 2: last_contacted_at cập nhật đúng
    // -----------------------------------------------------------------------
    public function test_create_interaction_updates_last_contacted_at(): void
    {
        $contactedAt = '2026-10-30T05:00:00+00:00';

        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'interested',
                'needs'        => ['ads'],
                'contacted_at' => $contactedAt,
            ]);

        $response->assertStatus(201);
        $this->assertNotNull($response->json('customer_summary.last_contacted_at'));
        $this->assertEquals('interested', $response->json('customer_summary.last_result'));
    }

    // -----------------------------------------------------------------------
    // Test 3: bản ghi interaction cũ không bị thay đổi
    // -----------------------------------------------------------------------
    public function test_create_interaction_old_record_unchanged(): void
    {
        $old = Interaction::create([
            'customer_id'  => $this->customer->id,
            'result'       => CallResult::NoAnswer,
            'note'         => 'Cuộc gọi cũ',
            'contacted_at' => Carbon::parse('2026-10-28T10:00:00+00:00'),
        ]);

        $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'interested',
                'needs'        => ['website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ])->assertStatus(201);

        // Bản ghi cũ không đổi
        $this->assertDatabaseHas('interactions', [
            'id'     => $old->id,
            'result' => 'no_answer',
            'note'   => 'Cuộc gọi cũ',
        ]);
        $this->assertDatabaseCount('interactions', 2);
    }

    // -----------------------------------------------------------------------
    // Test 4: GET list trả mới nhất trước
    // -----------------------------------------------------------------------
    public function test_list_interactions_newest_first(): void
    {
        Interaction::create([
            'customer_id'  => $this->customer->id,
            'result'       => CallResult::NoAnswer,
            'contacted_at' => Carbon::parse('2026-10-28T10:00:00+00:00'),
        ]);
        Interaction::create([
            'customer_id'  => $this->customer->id,
            'result'       => CallResult::Interested,
            'contacted_at' => Carbon::parse('2026-10-30T10:00:00+00:00'),
        ]);
        Interaction::create([
            'customer_id'  => $this->customer->id,
            'result'       => CallResult::Callback,
            'contacted_at' => Carbon::parse('2026-10-29T10:00:00+00:00'),
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson("/api/v1/customers/{$this->customer->id}/interactions");

        $response->assertStatus(200);
        $results = collect($response->json('data'))->pluck('result')->all();
        $this->assertSame(['interested', 'callback', 'no_answer'], $results);
    }

    // -----------------------------------------------------------------------
    // Test 5: needs rỗng → 422
    // -----------------------------------------------------------------------
    public function test_create_interaction_empty_needs_returns_422(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'callback',
                'needs'        => [],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['needs']);
    }

    // -----------------------------------------------------------------------
    // Test 6: result enum sai → 422
    // -----------------------------------------------------------------------
    public function test_create_interaction_invalid_result_enum_returns_422(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'xyz_invalid',
                'needs'        => ['website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['result']);
    }

    // -----------------------------------------------------------------------
    // Test 7: need_type enum sai → 422
    // -----------------------------------------------------------------------
    public function test_create_interaction_invalid_need_type_returns_422(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'callback',
                'needs'        => ['invalid_type'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['needs.0']);
    }

    // -----------------------------------------------------------------------
    // Test 8: khách archived → 422
    // -----------------------------------------------------------------------
    public function test_create_interaction_archived_customer_returns_422(): void
    {
        $this->customer->update(['status' => CustomerStatus::Archived]);

        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'callback',
                'needs'        => ['website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(422)
            ->assertJsonPath('code', 'CUSTOMER_ARCHIVED');
    }

    // -----------------------------------------------------------------------
    // Test 9: customer không tồn tại → 404
    // -----------------------------------------------------------------------
    public function test_create_interaction_nonexistent_customer_returns_404(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson('/api/v1/customers/999999/interactions', [
                'result'       => 'callback',
                'needs'        => ['website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(404);
    }

    // -----------------------------------------------------------------------
    // Test 10: remind_at ở quá khứ → 422
    // -----------------------------------------------------------------------
    public function test_create_interaction_past_remind_at_returns_422(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'callback',
                'needs'        => ['website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
                'follow_up'    => ['remind_at' => '2020-01-01T00:00:00+00:00'],
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['follow_up.remind_at']);
    }

    // -----------------------------------------------------------------------
    // Test 11: D6 — tạo interaction mới hoàn thành follow-up pending cũ
    // -----------------------------------------------------------------------
    public function test_create_interaction_completes_old_pending_follow_up_D6(): void
    {
        $oldFollowUp = FollowUp::create([
            'customer_id' => $this->customer->id,
            'remind_at'   => Carbon::now()->addDays(2),
            'status'      => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'interested',
                'needs'        => ['ads', 'website'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(201);

        // completed_follow_up_ids phải chứa id của follow-up cũ
        $this->assertContains($oldFollowUp->id, $response->json('completed_follow_up_ids'));

        // follow-up cũ phải thành completed trong DB
        $this->assertDatabaseHas('follow_ups', [
            'id'     => $oldFollowUp->id,
            'status' => 'completed',
        ]);
    }

    // -----------------------------------------------------------------------
    // Test 12: tạo follow-up mới khi có remind_at tương lai
    // -----------------------------------------------------------------------
    public function test_create_interaction_creates_new_follow_up_when_remind_at_given(): void
    {
        $remindAt = Carbon::now()->addDay()->toIso8601String();

        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'callback',
                'needs'        => ['software'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
                'follow_up'    => ['remind_at' => $remindAt],
            ]);

        $response->assertStatus(201);
        $this->assertNotNull($response->json('follow_up'));
        $this->assertEquals('pending', $response->json('follow_up.status'));
        $this->assertEquals($this->customer->id, $response->json('follow_up.customer_id'));

        $this->assertDatabaseHas('follow_ups', [
            'customer_id' => $this->customer->id,
            'status'      => 'pending',
        ]);
    }

    // -----------------------------------------------------------------------
    // Test 13: không có follow_up trong request → response follow_up = null
    // -----------------------------------------------------------------------
    public function test_create_interaction_no_follow_up_when_not_given(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                'result'       => 'no_answer',
                'needs'        => ['other'],
                'contacted_at' => '2026-10-30T03:00:00+00:00',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('follow_up', null);

        $this->assertDatabaseCount('follow_ups', 0);
    }

    // -----------------------------------------------------------------------
    // Test 14: rollback khi lỗi giữa chừng
    // -----------------------------------------------------------------------
    public function test_create_interaction_rollback_on_error(): void
    {
        // Buộc lỗi bằng cách mock InteractionService để ném exception sau bước tạo interaction
        $this->app->bind(\App\Services\InteractionService::class, function () {
            return new class extends \App\Services\InteractionService {
                public function record(\App\Models\Customer $customer, array $dto): array
                {
                    return DB::transaction(function () use ($customer) {
                        // Tạo interaction thật
                        Interaction::create([
                            'customer_id'  => $customer->id,
                            'result'       => CallResult::Callback,
                            'contacted_at' => now(),
                        ]);
                        // Ném exception → rollback
                        throw new \RuntimeException('Lỗi giả lập');
                    });
                }
            };
        });

        try {
            $this->withHeaders($this->authHeaders())
                ->postJson("/api/v1/customers/{$this->customer->id}/interactions", [
                    'result'       => 'callback',
                    'needs'        => ['website'],
                    'contacted_at' => '2026-10-30T03:00:00+00:00',
                ]);
        } catch (\Throwable) {
            // bắt exception từ mock
        }

        // Không có bản ghi nào được lưu
        $this->assertDatabaseCount('interactions', 0);
    }

    // -----------------------------------------------------------------------
    // Test 15: GET list có phân trang
    // -----------------------------------------------------------------------
    public function test_list_interactions_returns_paginated(): void
    {
        for ($i = 1; $i <= 5; $i++) {
            Interaction::create([
                'customer_id'  => $this->customer->id,
                'result'       => CallResult::NoAnswer,
                'contacted_at' => Carbon::now()->subDays($i),
            ]);
        }

        $response = $this->withHeaders($this->authHeaders())
            ->getJson("/api/v1/customers/{$this->customer->id}/interactions?per_page=2");

        $response->assertStatus(200)
            ->assertJsonCount(2, 'data')
            ->assertJsonPath('meta.total', 5);
    }
}
