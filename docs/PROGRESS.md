# PROGRESS

## Trạng thái

- **Task hiện tại:** Hoàn thành Giai đoạn 1: Backend T0-T6 (Sẵn sàng cho Giai đoạn 2: Android A0)
- **Đã xong:**
  - [x] T0: Khởi tạo repo và cài đặt môi trường (commit `b96aa78`, `7b3bf90`, `25dc469`)
  - [x] T1: Dựng Laravel 13 + MySQL + migrations + models + enums (30/10/2026)
    - Cài đặt Laravel 13.39.0 trong `backend/`
    - Cấu hình `.env` (MySQL DB `telesales`), `phpunit.xml` (DB `telesales_test`)
    - Tạo `backend/config/telesales.php` (business_timezone: Asia/Ho_Chi_Minh)
    - Tạo 5 Enums: CallResult, NeedType, PotentialLevel, CustomerStatus, FollowUpStatus
    - Tạo 4 migrations: customers, interactions, interaction_needs, follow_ups
    - Tạo 4 Eloquent models với fillable tường minh & enum casts
    - Test suite: 10 tests, 44 assertions xanh 100%
  - [x] T2: Auth token Sanctum + hạ tầng API (30/10/2026)
    - Cài đặt Sanctum v4.3.3, migration `personal_access_tokens`
    - Thêm trait `HasApiTokens` vào model `User`
    - Cấu hình JSON exception handler & guest redirection trong `bootstrap/app.php` chuẩn SRS mục 7
    - Tạo `UserSeeder` khởi tạo tài khoản telesales từ env
    - Tạo Artisan command `telesales:issue-token` cấp Bearer token qua CLI
    - Tạo route group `/api/v1` với middleware `auth:sanctum` và route `GET /api/v1/ping`
    - Test suite Feature: 15 tests, 60 assertions xanh 100% (AuthTest: 5 tests)
    - Đã kiểm tra thực tế bằng `curl` đến `/api/v1/ping` (401 khi thiếu/sai token, 200 khi có token)
  - [x] T3: PhoneNormalizer + Customers API (30/10/2026)
    - Tạo `PhoneNormalizer` theo SRS 5.2 (loại bỏ ký tự đặc biệt, chuẩn hóa +84/84/0, kiểm tra độ dài 10-11 số)
    - Tạo `DuplicatePhoneException` trả lỗi chuẩn HTTP 409 (`DUPLICATE_PHONE`, `existing_customer_id`)
    - Tạo `StoreCustomerRequest`, `UpdateCustomerRequest` validate theo FormRequest
    - Tạo `CustomerListResource`, `CustomerResource` format JSON chuẩn SRS 7.1
    - Tạo `CustomerService`: truy vấn danh sách phân trang tối ưu (tính `interaction_count`, `last_contacted_at`, `last_result` qua subqueries, chống N+1), tìm kiếm `q`, lọc theo status/potential, sắp xếp `last_contacted_at DESC NULLS LAST, id DESC`, transaction hủy pending follow-up khi lưu trữ
    - Tạo `CustomerController` và đăng ký routes trong `routes/api.php`
    - Test suite: 54 tests, 184 assertions xanh 100% (PhoneNormalizerTest: 25 tests, CustomerTest: 14 tests)
    - Thử nghiệm curl thật thành công: 201 tạo mới, 409 trùng số, 200 danh sách và chi tiết
  - [x] T4: Interactions API (30/10/2026)
    - Tạo `StoreInteractionRequest` validate result/needs enum, needs min:1, follow_up.remind_at sau now
    - Tạo `InteractionService::record` (DB transaction + lockForUpdate + D6): tạo interaction + needs, hoàn thành follow-up pending cũ, tạo follow-up mới nếu có remind_at
    - Tạo `InteractionResource` format GET list item
    - Tạo `InteractionController` (index + store), đăng ký route GET/POST /api/v1/customers/{id}/interactions
    - Test suite: 15 tests, 41 assertions xanh 100% (InteractionTest); toàn suite 69 tests, 225 assertions xanh
    - Thử nghiệm curl: POST không lịch (201), POST có lịch + D6 (201), GET list (200, mới nhất trước), needs rỗng (422), archived customer (422 CUSTOMER_ARCHIVED)
  - [x] T5: Follow-ups + Dashboard API (30/10/2026)
    - Tạo `BusinessTime` chuẩn hóa múi giờ Asia/Ho_Chi_Minh (UTC+7) -> UTC, tính ranh giới 00:00 và 23:59:59.999
    - Tạo `FollowUpNotPendingException` trả lỗi HTTP 409 chuẩn SRS khi thao tác lên follow-up không pending
    - Tạo `PatchFollowUpRequest` validate action (complete, cancel, reschedule) và remind_at tương lai
    - Tạo `FollowUpResource` format chuẩn SRS 7.3 (group, customer, note từ interaction tạo lịch)
    - Tạo `FollowUpService`: máy trạng thái apply() (complete/cancel/reschedule với lockForUpdate), list() lọc theo status và group (overdue, today, upcoming, all)
    - Tạo `FollowUpController`, đăng ký routes GET /api/v1/follow-ups và PATCH /api/v1/follow-ups/{id}
    - Tạo `DashboardService`: đếm overdue_count, today_count, potential_count (khách active có potential high/medium), lấy 20 items pending sắp xếp overdue trước rồi remind_at asc
    - Tạo `DashboardController`, đăng ký route GET /api/v1/dashboard
    - Test suite: BusinessTimeTest (7 tests), FollowUpTest (19 tests), DashboardTest (6 tests) xanh 100%
    - Toàn bộ backend test suite: 101 tests, 296 assertions xanh 100%
    - Thử nghiệm curl thật: GET follow-ups phân nhóm, GET dashboard, PATCH reschedule, complete, và 409 FOLLOW_UP_NOT_PENDING khi thao tác lịch đã xong
  - [x] T6: Đóng băng API contract (30/10/2026)
    - Chạy toàn bộ test suite backend: 101 tests, 296 assertions xanh 100%
    - Thực thi curl thực tế bao phủ đầy đủ tất cả endpoint (Ping, Customers, Interactions, Follow-ups, Dashboard, Error handling)
    - Xuất toàn bộ ví dụ request/response và bảng đối chiếu 1:1 với SRS mục 7 vào `docs/API_EXAMPLES.md`
    - Khẳng định 100% khớp đặc tả SRS mục 7, sẵn sàng làm cơ sở phát triển DTO và Repository cho client Android

## Cách chạy lại

- **Backend:** `cd backend && php artisan serve`
- **Cấp token telesales:** `cd backend && php artisan telesales:issue-token`
- **Chạy migrations dev:** `cd backend && php artisan migrate`
- **Test backend:** `cd backend && php artisan test`
- **Test T5 only:** `cd backend && php artisan test tests/Unit/BusinessTimeTest.php tests/Feature/FollowUpTest.php tests/Feature/DashboardTest.php`
- **Android build (sau A0):** `cd android && ./gradlew assembleDebug`
- **Android test (sau A0):** `cd android && ./gradlew test`

## Kết quả kiểm tra môi trường thực tế (sau T0)

```
php -v          → PHP 8.3.6 ✅
composer -V     → Composer 2.7.1 ✅
git --version   → git version 2.43.0 ✅
mysql --version → mysql Ver 8.0.46 for Linux on x86_64 ✅
java -version   → openjdk version 17.0.20.1 ✅
php -m (mysql)  → mysqli, pdo_mysql ✅
PDO connection  → SUCCESS (telesales & telesales_test DB ready) ✅
```

## Quyết định mới (xem thêm DECISIONS.md)

- AlarmManager thay WorkManager (D-ARCH-01)
- docs/SRS.md = nguồn đặc tả chính (D-REPO-01)
- Android Studio cài trên Windows (D-ENV-01)
- DB dev: `telesales`, DB test: `telesales_test`, user: `telesales` / `secret`

## Việc phát hiện thêm (ngoài phạm vi, chưa làm)

- Android Studio Narwhal 2025.1.1 cần được cài đặt trên Windows trước khi tiến hành Giai đoạn 2 (A0).

## Việc tiếp theo

1. Chờ người dùng duyệt đóng băng API contract.
2. Bắt đầu Giai đoạn 2 — Android (Task A0: Dựng dự án Android Compose).
