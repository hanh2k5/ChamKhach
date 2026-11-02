# PROGRESS

## Nguồn đặc tả

- `docs/TELESALES_SPEC.md` là nguồn đặc tả chính; `docs/SRS.md` là tài liệu SRS gốc để tham chiếu (D-REPO-01).
- Đã thêm spec đầy đủ (604 dòng) trong commit `8447ecd` (`docs: add detailed spec`).

## Trạng thái

- **Task hiện tại:** Hoàn thành A1; build/test và kiểm thử emulator đã được xác minh bởi người dùng.
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
  - [x] A0: Dựng dự án Android Compose (30/10/2026)
    - Cài đặt Android SDK cmdline-tools 12.0, platform-tools, platforms;android-35, build-tools;35.0.0 vào `/home/acer/android-sdk` trong WSL2
    - Thiết lập cơ chế tự động tránh xung đột `local.properties` giữa WSL2 Linux và Windows Android Studio trong `settings.gradle.kts`
    - Cấu hình Gradle Wrapper 8.10.2, AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.10.01 (Material 3)
    - Cấu hình dependencies: Room 2.6.1, DataStore 1.1.1, Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization 1.7.3
    - Cấu hình `networkSecurityConfig` debug cho phép HTTP cleartext 10.0.2.2/localhost, `allowBackup=false`, Proguard keep rules cho DTO
    - Xây dựng cấu trúc `AppContainer` (Manual DI) và `MainActivity` Compose
    - Build thực tế thành công: `./gradlew assembleDebug` (ra file `app-debug.apk` 11MB) và `./gradlew testDebugUnitTest` xanh 100%

## Cách chạy lại

- **Backend:** `cd backend && php artisan serve`
- **Cấp token telesales:** `cd backend && php artisan telesales:issue-token`
- **Chạy migrations dev:** `cd backend && php artisan migrate`
- **Test backend:** `cd backend && php artisan test`
- **Test T5 only:** `cd backend && php artisan test tests/Unit/BusinessTimeTest.php tests/Feature/FollowUpTest.php tests/Feature/DashboardTest.php`
- **Android build:** `cd android && ./gradlew assembleDebug`
- **Android test:** `cd android && ./gradlew testDebugUnitTest`

## A1 — Tầng mạng + Cài đặt (hoàn thành)

- Đã thêm Preferences DataStore cho Base URL/token; Base URL được trim, chỉ nhận `http://`/`https://`, tự thêm dấu `/` cuối và từ chối URL sai trước khi gọi mạng.
- Đã thêm Retrofit `ApiService`/`PingDto`, OkHttp `AuthInterceptor` gắn Bearer + `Accept: application/json`, và cấu hình JSON bỏ qua field response chưa biết. Không dùng logging interceptor, không log token.
- Đã thêm màn Cài đặt tiếng Việt: Base URL, Token ẩn ký tự, Lưu, Kiểm tra kết nối và trạng thái rõ ràng (thành công, token sai, URL/endpoint sai, mạng/máy chủ/lưu lỗi).
- Người dùng xác minh thành công: `cd android && ./gradlew clean testDebugUnitTest assembleDebug`; 12 tests thuộc 5 test classes, không lỗi.
- Người dùng kiểm thử thủ công trên emulator: kết nối thành công; token sai báo lỗi; backend tắt báo lỗi mạng; cấu hình còn sau khi mở lại app.
- Lệnh chạy lại riêng: `cd android && ./gradlew testDebugUnitTest` và `cd android && ./gradlew assembleDebug`.

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
- docs/TELESALES_SPEC.md = nguồn đặc tả chính; docs/SRS.md = tài liệu SRS tham chiếu (D-REPO-01)
- Android Studio cài trên Windows (D-ENV-01)
- DB dev: `telesales`, DB test: `telesales_test`, user: `telesales` / `secret`

## Việc phát hiện thêm (ngoài phạm vi, chưa làm)

- Android Studio Narwhal 2025.1.1 cần được cài đặt trên Windows trước khi tiến hành Giai đoạn 2 (A0).
- `docs/API_EXAMPLES.md` vẫn ghi `docs/SRS.md` là nguồn đặc tả; cần cập nhật chú thích sang `docs/TELESALES_SPEC.md`.
- Cảnh báo lint `Check for instance is always true` tại `android/app/src/test/java/com/telesales/assistant/AppContainerTest.kt:25`.

## Việc tiếp theo

1. Task kế tiếp theo kế hoạch là A2; chưa bắt đầu trong phiên này.
