# PROGRESS

## Trạng thái

- **Task hiện tại:** Hoàn thành T2 (sẵn sàng cho T3)
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

## Cách chạy lại

- **Backend:** `cd backend && php artisan serve`
- **Cấp token telesales:** `cd backend && php artisan telesales:issue-token`
- **Chạy migrations dev:** `cd backend && php artisan migrate`
- **Test backend:** `cd backend && php artisan test`
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

1. Nhận xác nhận hoàn thành T2 từ người dùng.
2. Bắt đầu task T3 (PhoneNormalizer + Customers API).
