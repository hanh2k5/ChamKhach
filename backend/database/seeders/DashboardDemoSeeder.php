<?php

namespace Database\Seeders;

use App\Enums\CustomerStatus;
use App\Enums\FollowUpStatus;
use App\Enums\PotentialLevel;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Support\BusinessTime;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class DashboardDemoSeeder extends Seeder
{
    public function run(): void
    {
        DB::transaction(function (): void {
            $now = BusinessTime::now();
            $todayEnd = BusinessTime::todayEndUtc($now);

            // Bắt đầu dải Hôm nay cách hiện tại ít nhất 15 phút để số liệu giữ nguyên 1000 trong suốt phiên làm việc
            $todayStart = $now->addMinutes(15);
            $todayWindowSeconds = max(300, $todayStart->diffInSeconds($todayEnd->subMinutes(2)));

            $demoCustomerIds = Customer::where('source', 'Dữ liệu mẫu Dashboard')->pluck('id');
            FollowUp::whereIn('customer_id', $demoCustomerIds)->delete();
            Customer::whereIn('id', $demoCustomerIds)->delete();

            // 1. 20 khách Quá hạn (1..20): remind_at < $now, potential: Unrated
            // 2. 20 khách Hôm nay (21..40): remind_at trong khoảng [todayStart, todayEnd), potential: Unrated
            // 3. 20 khách Tiềm năng (41..60): potential: High / Medium, remind_at > $todayEnd
            $totalCount = 60;
            $phones = collect(range(1, $totalCount))
                ->map(fn (int $number): string => '0988' . str_pad((string) $number, 6, '0', STR_PAD_LEFT));

            foreach ($phones as $index => $phone) {
                $number = $index + 1;

                if ($number <= 20) {
                    $potentialLevel = PotentialLevel::Unrated;
                    $remindAt = $now->subMinutes(21 - $number);
                } elseif ($number <= 40) {
                    $offset = $number - 20;
                    $potentialLevel = PotentialLevel::Unrated;
                    $remindAt = $todayStart->addSeconds(
                        (int) ($todayWindowSeconds * $offset / 21)
                    );
                } else {
                    $offset = $number - 40;
                    $potentialLevel = ($offset % 2 === 0)
                        ? PotentialLevel::High
                        : PotentialLevel::Medium;
                    $remindAt = $todayEnd->addDays(($offset % 30) + 1)->addMinutes($offset);
                }

                $customer = Customer::create([
                    'company_name' => 'Khách Dashboard ' . $number,
                    'contact_name' => 'Người liên hệ ' . $number,
                    'phone' => $phone,
                    'phone_normalized' => $phone,
                    'address' => 'Hà Nội',
                    'industry' => 'Dịch vụ',
                    'source' => 'Dữ liệu mẫu Dashboard',
                    'potential_level' => $potentialLevel,
                    'status' => CustomerStatus::Active,
                ]);

                FollowUp::create([
                    'customer_id' => $customer->id,
                    'remind_at' => $remindAt,
                    'status' => FollowUpStatus::Pending,
                ]);
            }
        });
    }
}
