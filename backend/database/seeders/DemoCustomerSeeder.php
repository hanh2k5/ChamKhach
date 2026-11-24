<?php

namespace Database\Seeders;

use App\Enums\FollowUpStatus;
use App\Enums\PotentialLevel;
use App\Models\Customer;
use App\Models\FollowUp;
use App\Support\BusinessTime;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class DemoCustomerSeeder extends Seeder
{
    public function run(): void
    {
        DB::transaction(function (): void {
            // Xóa sạch dữ liệu cũ để chỉ giữ đúng 20 khách hàng duy nhất
            FollowUp::query()->delete();
            Customer::query()->delete();

            $now = BusinessTime::now();

            $customers = [
                ['Công ty An Phát', 'Nguyễn Minh Anh', '0901000001', PotentialLevel::High, $now->subDays(2)],
                ['Nội thất Gia Khang', 'Trần Quốc Bảo', '0901000002', PotentialLevel::Medium, $now->subDay()],
                ['Thời trang Mộc Lan', 'Lê Hoài Nam', '0901000003', PotentialLevel::High, $now->subHours(5)],
                ['Dược phẩm Việt Tín', 'Phạm Thu Hà', '0901000004', PotentialLevel::Medium, $now->subHour()],
                ['Giải pháp Số Sao Việt', 'Vũ Đức Long', '0901000005', PotentialLevel::High, $now->subMinutes(30)],
                ['Nhà hàng Hương Sen', 'Đỗ Ngọc Mai', '0901000006', PotentialLevel::Medium, $now->subMinutes(10)],
                ['Vận tải Thành Công', 'Bùi Văn Hùng', '0901000007', PotentialLevel::Medium, $now->addMinutes(30)],
                ['Giáo dục Ánh Dương', 'Hoàng Khánh Linh', '0901000008', PotentialLevel::Medium, $now->addHour()],
                ['Điện máy Nam Việt', 'Ngô Tuấn Kiệt', '0901000009', PotentialLevel::High, $now->addHours(2)],
                ['Nông sản Xanh', 'Đặng Thùy Dương', '0901000010', PotentialLevel::Medium, $now->addHours(3)],
                ['Xây dựng Phú Quý', 'Mai Thành Đạt', '0901000011', PotentialLevel::High, $now->addHours(4)],
                ['Studio Mây Trắng', 'Nguyễn Hà My', '0901000012', PotentialLevel::Medium, $now->addHours(5)],
                ['Nội thất Bình Minh', 'Trương Gia Huy', '0901000013', PotentialLevel::High, $now->addDays(2)],
                ['Mỹ phẩm Ngọc Lan', 'Phan Bảo Châu', '0901000014', PotentialLevel::Medium, $now->addDays(3)],
                ['Cà phê Đồi Gió', 'Lý Thanh Tùng', '0901000015', PotentialLevel::High, $now->addDays(4)],
                ['Cơ khí Đại Thành', 'Đinh Văn Nam', '0901000016', PotentialLevel::Medium, $now->addDays(5)],
                ['Logistics Minh Châu', 'Cao Hoàng Yến', '0901000017', PotentialLevel::High, $now->addDays(6)],
                ['Bất động sản Phúc An', 'Tạ Anh Tuấn', '0901000018', PotentialLevel::Medium, $now->addDays(7)],
                ['Thực phẩm Tâm Việt', 'Hồ Ngọc Diệp', '0901000019', PotentialLevel::High, $now->addDays(8)],
                ['Thiết bị Văn phòng 365', 'Lâm Quốc Việt', '0901000020', PotentialLevel::Medium, $now->addDays(9)],
            ];

            foreach ($customers as [$companyName, $contactName, $phone, $potential, $remindAt]) {
                $c = Customer::create([
                    'company_name' => $companyName,
                    'contact_name' => $contactName,
                    'phone' => $phone,
                    'phone_normalized' => $phone,
                    'address' => 'Hà Nội',
                    'industry' => 'Dịch vụ',
                    'source' => 'Dữ liệu mẫu',
                    'potential_level' => $potential,
                    'status' => 'active',
                ]);

                FollowUp::create([
                    'customer_id' => $c->id,
                    'remind_at' => $remindAt,
                    'status' => FollowUpStatus::Pending,
                ]);
            }
        });
    }
}
