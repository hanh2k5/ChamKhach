<?php

namespace Tests\Feature;

use App\Enums\CallResult;
use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Models\Interaction;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

class CustomerTest extends TestCase
{
    use RefreshDatabase;

    protected User $user;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = User::factory()->create([
            'email' => 'agent@test.local',
        ]);
    }

    protected function authHeaders(): array
    {
        $token = $this->user->createToken('test')->plainTextToken;
        return [
            'Authorization' => 'Bearer '.$token,
            'Accept' => 'application/json',
        ];
    }

    public function test_create_customer_success_with_only_phone(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->postJson('/api/v1/customers', [
                'phone' => '0912 345 678',
            ]);

        $response        ->assertStatus(201)
        ->assertJsonPath('data.phone', '0912 345 678')
        ->assertJsonPath('data.phone_normalized', '0912345678')
        ->assertJsonPath('data.status', 'active')
            ->assertJsonPath('data.interaction_count', 0)
            ->assertJsonPath('data.last_contacted_at', null)
            ->assertJsonPath('data.last_result', null);

        $this->assertDatabaseHas('customers', [
            'phone_normalized' => '0912345678',
            'status' => 'active',
        ]);
    }

    public function test_customer_list_filters_potential_customers_and_keeps_descending_order(): void
    {
        $customers = collect(range(1, 21))->map(function (int $number): Customer {
            return Customer::create([
                'company_name' => 'Khách '.$number,
                'phone' => '090200'.str_pad((string) $number, 4, '0', STR_PAD_LEFT),
                'phone_normalized' => '090200'.str_pad((string) $number, 4, '0', STR_PAD_LEFT),
                'potential_level' => 'high',
                'status' => CustomerStatus::Active,
            ]);
        });

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/customers?potential=high,medium&per_page=20&page=2');

        $response->assertOk()
            ->assertJsonPath('meta.current_page', 2)
            ->assertJsonPath('meta.last_page', 2)
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $customers->first()->id);
    }

    public function test_create_customer_success_with_full_fields(): void
    {
        $payload = [
            'company_name' => 'Công ty ABC',
            'contact_name' => 'Nguyễn Văn B',
            'phone' => '+84987654321',
            'address' => 'Hà Nội',
            'industry' => 'Công nghệ',
            'source' => 'Google',
        ];

        $response = $this->withHeaders($this->authHeaders())
            ->postJson('/api/v1/customers', $payload);

        $response->assertStatus(201)
            ->assertJsonPath('data.company_name', 'Công ty ABC')
            ->assertJsonPath('data.contact_name', 'Nguyễn Văn B')
            ->assertJsonPath('data.phone', '+84987654321')
            ->assertJsonPath('data.phone_normalized', '0987654321');
    }

    public function test_create_customer_validation_fails(): void
    {
        // Thiếu phone
        $res1 = $this->withHeaders($this->authHeaders())
            ->postJson('/api/v1/customers', []);

        $res1->assertStatus(422)
            ->assertJsonValidationErrors(['phone']);

        // Phone không hợp lệ
        $res2 = $this->withHeaders($this->authHeaders())
            ->postJson('/api/v1/customers', [
                'phone' => '0912abc678',
            ]);

        $res2->assertStatus(422)
            ->assertJsonValidationErrors(['phone']);
    }

    public function test_create_and_update_customer_workflow_fields(): void
    {
        $created = $this->withHeaders($this->authHeaders())->postJson('/api/v1/customers', [
            'phone' => '0901234567',
            'active_since' => '5 năm',
            'industry' => 'Xây dựng',
            'needs' => ['website', 'ads'],
        ])->assertCreated()
            ->assertJsonPath('data.active_since', '5 năm')
            ->assertJsonPath('data.industry', 'Xây dựng')
            ->assertJsonPath('data.needs', ['website', 'ads']);

        $id = $created->json('data.id');
        $this->withHeaders($this->authHeaders())->patchJson("/api/v1/customers/{$id}", [
            'needs' => ['software'],
            'active_since' => null,
        ])->assertOk()
            ->assertJsonPath('data.needs', ['software'])
            ->assertJsonPath('data.active_since', null);
    }

    public function test_create_customer_duplicate_phone_returns_409(): void
    {
        $first = Customer::create([
            'contact_name' => 'Khách cũ',
            'phone' => '0912345678',
            'phone_normalized' => '0912345678',
            'status' => CustomerStatus::Active,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->postJson('/api/v1/customers', [
                'phone' => '+84 912 345 678', // cùng số sau chuẩn hóa
                'contact_name' => 'Khách mới',
            ]);

        $response->assertStatus(409)
            ->assertExactJson([
                'message' => 'Số điện thoại đã tồn tại.',
                'code' => 'DUPLICATE_PHONE',
                'existing_customer_id' => $first->id,
            ]);
    }

    public function test_show_customer_returns_details_and_aggregates(): void
    {
        $customer = Customer::create([
            'company_name' => 'Tech Corp',
            'phone' => '0901112233',
            'phone_normalized' => '0901112233',
            'status' => CustomerStatus::Active,
        ]);

        // Tạo 2 interactions
        Interaction::create([
            'customer_id' => $customer->id,
            'result' => CallResult::NoAnswer,
            'note' => 'Không nghe máy',
            'contacted_at' => Carbon::parse('2026-10-28 10:00:00'),
        ]);

        Interaction::create([
            'customer_id' => $customer->id,
            'result' => CallResult::Interested,
            'note' => 'Quan tâm',
            'contacted_at' => Carbon::parse('2026-10-29 14:00:00'),
        ]);

        // Tạo follow-up pending
        $followUp = FollowUp::create([
            'customer_id' => $customer->id,
            'remind_at' => Carbon::parse('2026-10-31 09:00:00'),
            'status' => FollowUpStatus::Pending,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson("/api/v1/customers/{$customer->id}");

        $response->assertStatus(200)
            ->assertJsonPath('data.id', $customer->id)
            ->assertJsonPath('data.interaction_count', 2)
            ->assertJsonPath('data.last_result', 'interested')
            ->assertJsonPath('data.next_follow_up.id', $followUp->id);
    }

    public function test_show_customer_not_found_returns_404(): void
    {
        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/customers/999999');

        $response->assertStatus(404)
            ->assertExactJson([
                'message' => 'Not found.',
            ]);
    }

    public function test_update_customer_success(): void
    {
        $customer = Customer::create([
            'phone' => '0901112233',
            'phone_normalized' => '0901112233',
            'status' => CustomerStatus::Active,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/customers/{$customer->id}", [
                'company_name' => 'Công ty Mới',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('data.company_name', 'Công ty Mới');

        $this->assertDatabaseHas('customers', [
            'id' => $customer->id,
            'company_name' => 'Công ty Mới',
        ]);
    }

    public function test_update_customer_keeps_same_phone_success(): void
    {
        $customer = Customer::create([
            'phone' => '0901112233',
            'phone_normalized' => '0901112233',
            'status' => CustomerStatus::Active,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/customers/{$customer->id}", [
                'phone' => '0901 112 233',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('data.phone_normalized', '0901112233');
    }

    public function test_update_customer_duplicate_phone_returns_409(): void
    {
        $c1 = Customer::create([
            'phone' => '0901111111',
            'phone_normalized' => '0901111111',
            'status' => CustomerStatus::Active,
        ]);

        $c2 = Customer::create([
            'phone' => '0902222222',
            'phone_normalized' => '0902222222',
            'status' => CustomerStatus::Active,
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->patchJson("/api/v1/customers/{$c2->id}", [
                'phone' => '0901111111',
            ]);

        $response->assertStatus(409)
            ->assertExactJson([
                'message' => 'Số điện thoại đã tồn tại.',
                'code' => 'DUPLICATE_PHONE',
                'existing_customer_id' => $c1->id,
            ]);
    }

    public function test_delete_customer_permanently_cascades_related_records(): void
    {
        $customer = Customer::create([
            'phone' => '0901112244',
            'phone_normalized' => '0901112244',
            'status' => CustomerStatus::Active,
        ]);
        $interaction = Interaction::create([
            'customer_id' => $customer->id,
            'result' => CallResult::NoAnswer,
            'contacted_at' => Carbon::now(),
        ]);
        DB::table('interaction_needs')->insert([
            'interaction_id' => $interaction->id,
            'need_type' => 'website',
        ]);
        $followUp = FollowUp::create([
            'customer_id' => $customer->id,
            'interaction_id' => $interaction->id,
            'remind_at' => Carbon::now()->addDay(),
            'status' => FollowUpStatus::Pending,
        ]);

        $this->withHeaders($this->authHeaders())
            ->deleteJson("/api/v1/customers/{$customer->id}")
            ->assertOk()
            ->assertJsonPath('data.deleted_follow_up_ids.0', $followUp->id);

        $this->assertDatabaseMissing('customers', ['id' => $customer->id]);
        $this->assertDatabaseMissing('interactions', ['id' => $interaction->id]);
        $this->assertDatabaseMissing('interaction_needs', ['interaction_id' => $interaction->id]);
        $this->assertDatabaseMissing('follow_ups', ['id' => $followUp->id]);
    }

    public function test_list_customers_includes_all_existing_customers_without_archive_filter(): void
    {
        foreach ([CustomerStatus::Active, CustomerStatus::Archived] as $index => $status) {
            Customer::create([
                'company_name' => 'Customer '.$index,
                'phone' => '090100000'.($index + 1),
                'phone_normalized' => '090100000'.($index + 1),
                'status' => $status,
            ]);
        }

        $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/customers')
            ->assertOk()
            ->assertJsonCount(2, 'data');
    }

    public function test_list_customers_search_and_filters(): void
    {
        Customer::create([
            'company_name' => 'FPT Software',
            'contact_name' => 'Lê Văn A',
            'phone' => '0912345678',
            'phone_normalized' => '0912345678',
            'status' => CustomerStatus::Active,
        ]);

        Customer::create([
            'company_name' => 'Viettel Telecom',
            'contact_name' => 'Trần Thị B',
            'phone' => '0988776655',
            'phone_normalized' => '0988776655',
            'status' => CustomerStatus::Active,
        ]);

        // Tìm theo tên công ty
        $res1 = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/customers?q=FPT');
        $res1->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.company_name', 'FPT Software');

        // Tìm theo SĐT
        $res2 = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/customers?q=0988');
        $res2->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.company_name', 'Viettel Telecom');

    }

    public function test_list_customers_sort_order_last_contacted_desc_nulls_last(): void
    {
        $cNoCall = Customer::create([
            'company_name' => 'Chưa gọi',
            'phone' => '0901000001',
            'phone_normalized' => '0901000001',
            'status' => CustomerStatus::Active,
        ]);

        $cCallOld = Customer::create([
            'company_name' => 'Gọi cũ',
            'phone' => '0901000002',
            'phone_normalized' => '0901000002',
            'status' => CustomerStatus::Active,
        ]);
        Interaction::create([
            'customer_id' => $cCallOld->id,
            'result' => CallResult::NoAnswer,
            'contacted_at' => Carbon::parse('2026-10-20 10:00:00'),
        ]);

        $cCallNew = Customer::create([
            'company_name' => 'Gọi mới',
            'phone' => '0901000003',
            'phone_normalized' => '0901000003',
            'status' => CustomerStatus::Active,
        ]);
        Interaction::create([
            'customer_id' => $cCallNew->id,
            'result' => CallResult::Interested,
            'contacted_at' => Carbon::parse('2026-10-29 10:00:00'),
        ]);

        $response = $this->withHeaders($this->authHeaders())
            ->getJson('/api/v1/customers');

        $response->assertStatus(200);
        $names = collect($response->json('data'))->pluck('company_name')->all();

        // Kỳ vọng: Gọi mới trước, Gọi cũ tiếp theo, Chưa gọi (null) ở cuối
        $this->assertSame(['Gọi mới', 'Gọi cũ', 'Chưa gọi'], $names);
    }

    public function test_list_customers_no_n_plus_one_query(): void
    {
        $service = app(\App\Services\CustomerService::class);

        // Tạo 3 khách hàng có interactions
        for ($i = 1; $i <= 3; $i++) {
            $c = Customer::create([
                'company_name' => "Company {$i}",
                'phone' => "090100000{$i}",
                'phone_normalized' => "090100000{$i}",
                'status' => CustomerStatus::Active,
            ]);
            Interaction::create([
                'customer_id' => $c->id,
                'result' => CallResult::Callback,
                'contacted_at' => Carbon::now()->subDays($i),
            ]);
        }

        DB::flushQueryLog();
        DB::enableQueryLog();
        $res3 = $service->list([]);
        $res3->items();
        $queryCountFor3 = count(DB::getQueryLog());
        DB::disableQueryLog();

        // Tạo thêm 20 khách hàng nữa có interactions
        for ($i = 4; $i <= 23; $i++) {
            $c = Customer::create([
                'company_name' => "Company {$i}",
                'phone' => "09010000{$i}",
                'phone_normalized' => "09010000{$i}",
                'status' => CustomerStatus::Active,
            ]);
            Interaction::create([
                'customer_id' => $c->id,
                'result' => CallResult::Callback,
                'contacted_at' => Carbon::now()->subDays($i),
            ]);
        }

        DB::flushQueryLog();
        DB::enableQueryLog();
        $res23 = $service->list([]);
        $res23->items();
        $queryCountFor23 = count(DB::getQueryLog());
        DB::disableQueryLog();

        // Khẳng định số query là đúng 2 câu (1 count + 1 select) và không tăng theo số bản ghi
        $this->assertSame(2, $queryCountFor3);
        $this->assertSame(2, $queryCountFor23);
    }
}
