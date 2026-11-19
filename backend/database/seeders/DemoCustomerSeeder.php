<?php

namespace Database\Seeders;

use App\Models\Customer;
use Illuminate\Database\Seeder;

class DemoCustomerSeeder extends Seeder
{
    public function run(): void
    {
        $customers = [
            ['Công ty An Phát', 'Nguyễn Minh Anh', '0901000001'],
            ['Nội thất Gia Khang', 'Trần Quốc Bảo', '0901000002'],
            ['Thời trang Mộc Lan', 'Lê Hoài Nam', '0901000003'],
            ['Dược phẩm Việt Tín', 'Phạm Thu Hà', '0901000004'],
            ['Giải pháp Số Sao Việt', 'Vũ Đức Long', '0901000005'],
            ['Nhà hàng Hương Sen', 'Đỗ Ngọc Mai', '0901000006'],
            ['Vận tải Thành Công', 'Bùi Văn Hùng', '0901000007'],
            ['Giáo dục Ánh Dương', 'Hoàng Khánh Linh', '0901000008'],
            ['Điện máy Nam Việt', 'Ngô Tuấn Kiệt', '0901000009'],
            ['Nông sản Xanh', 'Đặng Thùy Dương', '0901000010'],
            ['Xây dựng Phú Quý', 'Mai Thành Đạt', '0901000011'],
            ['Studio Mây Trắng', 'Nguyễn Hà My', '0901000012'],
            ['Nội thất Bình Minh', 'Trương Gia Huy', '0901000013'],
            ['Mỹ phẩm Ngọc Lan', 'Phan Bảo Châu', '0901000014'],
            ['Cà phê Đồi Gió', 'Lý Thanh Tùng', '0901000015'],
            ['Cơ khí Đại Thành', 'Đinh Văn Nam', '0901000016'],
            ['Logistics Minh Châu', 'Cao Hoàng Yến', '0901000017'],
            ['Bất động sản Phúc An', 'Tạ Anh Tuấn', '0901000018'],
            ['Thực phẩm Tâm Việt', 'Hồ Ngọc Diệp', '0901000019'],
            ['Thiết bị Văn phòng 365', 'Lâm Quốc Việt', '0901000020'],
        ];

        foreach ($customers as [$companyName, $contactName, $phone]) {
            Customer::updateOrCreate(
                ['phone_normalized' => $phone],
                [
                    'company_name' => $companyName,
                    'contact_name' => $contactName,
                    'phone' => $phone,
                    'phone_normalized' => $phone,
                    'address' => 'Hà Nội',
                    'industry' => 'Dịch vụ',
                    'source' => 'Dữ liệu mẫu',
                    'status' => 'active',
                ],
            );
        }
    }
}
