# VERSIONS — Phiên bản đã ghim

> Tạo lần đầu: 30/10/2026 (task T0). Cập nhật khi cài đặt thực tế.
> Nguyên tắc: không dùng `latest`, `^`, `*`, `+`. Chỉ dùng phiên bản ổn định (không alpha/beta/RC).

## Backend

| Thành phần        | Phiên bản đã chọn | Nguồn tra (ngày 30/10/2026)                          |
|-------------------|-------------------|------------------------------------------------------|
| PHP               | 8.3.6             | `php -v` trên máy                                    |
| Composer          | 2.7.1             | `composer -V` trên máy                               |
| Laravel Framework | 13.39.0           | `php artisan --version` (laravel/framework)          |
| Laravel Sanctum   | đi kèm Laravel 13 | Cài qua `composer create-project` mặc định           |
| MySQL             | 8.4.7 LTS         | mysql.com — bản LTS, release 21/10/2025              |
| Test framework    | Pest (mặc định Laravel 13) | Chọn một, không trộn PHPUnit thuần       |

## Android

| Thành phần            | Phiên bản đã chọn     | Nguồn tra (ngày 30/10/2026)            |
|-----------------------|-----------------------|----------------------------------------|
| Android Studio        | Narwhal 2025.1.1      | android.com — bản stable mới nhất 2025 |
| JDK                   | 17                    | Yêu cầu tối thiểu của AGP 8.x         |
| Kotlin                | 2.4.21                | kotlinlang.org — stable tháng 10/2026  |
| Android Gradle Plugin | 8.13.2                | android.com                            |
| Gradle                | 8.13                  | android.com (ghép với AGP 8.13)        |
| Jetpack Compose BOM   | 2026.08.00            | googleblog.com — stable tháng 8/2026   |
| compileSdk / targetSdk| API 36 (Android 16)   | AGP 8.13 hỗ trợ tối đa API 36         |
| minSdk                | 26 (Android 8.0)      | SPEC mục 2.1                           |
| KSP                   | 2.4.21-1.0.x          | Tiền tố Kotlin phải khớp (tra lại khi tạo project A0) |

## Phiên bản thực tế trên máy sau cài đặt

> Điền vào sau khi hoàn tất cài đặt môi trường (cuối phiên T0, cập nhật phiên T1).

| Công cụ           | Phiên bản cài được | Ngày cài   | Ghi chú                                   |
|-------------------|--------------------|------------|-------------------------------------------|
| PHP               | 8.3.6              | 30/10/2026 | `php -v`                                  |
| Composer          | 2.7.1              | 30/10/2026 | `composer -V`                             |
| Laravel Framework | 13.39.0            | 30/10/2026 | `php artisan --version` (project backend) |
| MySQL             | 8.0.46             | 30/10/2026 | Service active, DB `telesales` & `telesales_test` đã tạo |
| JDK               | OpenJDK 17.0.20.1  | 30/10/2026 | Đã có java, javac, jar                    |
| php-mysql         | pdo_mysql & mysqli | 30/10/2026 | Kết nối PDO MySQL kiểm tra thành công     |
| Android Studio    | (cài trên Windows) | —          | Cần cài trên Windows trước task A0        |
