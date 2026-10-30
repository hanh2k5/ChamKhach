# PROGRESS

## Trạng thái

- **Task hiện tại:** Hoàn thành T0 (sẵn sàng cho T1)
- **Đã xong:**
  - [x] Tạo cấu trúc thư mục `backend/`, `android/`, `docs/` (30/10/2026)
  - [x] Tạo `.gitignore` (Laravel + Android)
  - [x] Tạo `docs/VERSIONS.md` (phiên bản đã tra & cập nhật phiên bản thực tế)
  - [x] Tạo `docs/DECISIONS.md`
  - [x] Tạo `docs/PROGRESS.md`
  - [x] Cài đặt MySQL 8.0.46, OpenJDK 17.0.20.1, php8.3-mysql
  - [x] Tạo database `telesales`, `telesales_test` & user `telesales` (password: `secret`)
  - [x] Test kết nối PDO MySQL thành công từ PHP CLI
  - [x] Commit đầu tiên `b96aa78`

## Cách chạy lại

- **Backend (sau T1):** `cd backend && php artisan serve`
- **Test backend (sau T1):** `cd backend && php artisan test`
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

1. Nhận xác nhận hoàn thành T0 từ người dùng.
2. Bắt đầu task T1 (Dựng Laravel + MySQL + migrations + models).
