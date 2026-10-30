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
use Illuminate\Database\QueryException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ModelsTest extends TestCase
{
    use RefreshDatabase;

    public function test_can_create_customer_with_enum_casts(): void
    {
        $customer = Customer::create([
            'company_name' => 'Công ty ABC',
            'contact_name' => 'Nguyễn Văn A',
            'phone' => '0901234567',
            'phone_normalized' => '0901234567',
            'address' => 'Hà Nội',
            'industry' => 'Công nghệ',
            'source' => 'Facebook',
            'potential_level' => PotentialLevel::High,
            'status' => CustomerStatus::Active,
        ]);

        $this->assertDatabaseHas('customers', [
            'id' => $customer->id,
            'phone_normalized' => '0901234567',
            'potential_level' => 'high',
            'status' => 'active',
        ]);

        $fresh = $customer->fresh();
        $this->assertSame(PotentialLevel::High, $fresh->potential_level);
        $this->assertSame(CustomerStatus::Active, $fresh->status);
    }

    public function test_customer_phone_normalized_must_be_unique(): void
    {
        Customer::create([
            'phone' => '0901234567',
            'phone_normalized' => '0901234567',
        ]);

        $this->expectException(QueryException::class);

        Customer::create([
            'phone' => '0901234567',
            'phone_normalized' => '0901234567',
        ]);
    }

    public function test_can_create_interaction_with_needs_and_follow_up(): void
    {
        $customer = Customer::create([
            'phone' => '0912345678',
            'phone_normalized' => '0912345678',
        ]);

        $interaction = Interaction::create([
            'customer_id' => $customer->id,
            'result' => CallResult::Callback,
            'note' => 'Hẹn gọi lại chiều mai',
            'contacted_at' => now(),
        ]);

        InteractionNeed::create([
            'interaction_id' => $interaction->id,
            'need_type' => NeedType::Website,
        ]);

        InteractionNeed::create([
            'interaction_id' => $interaction->id,
            'need_type' => NeedType::Ads,
        ]);

        $followUp = FollowUp::create([
            'customer_id' => $customer->id,
            'interaction_id' => $interaction->id,
            'remind_at' => now()->addDay(),
            'status' => FollowUpStatus::Pending,
        ]);

        $this->assertCount(1, $customer->fresh()->interactions);
        $this->assertCount(2, $interaction->fresh()->needs);
        $this->assertCount(1, $customer->fresh()->followUps);
        $this->assertSame($customer->id, $followUp->customer->id);
        $this->assertSame($interaction->id, $followUp->interaction->id);
    }
}
