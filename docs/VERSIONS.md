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
| Android Studio        | Narwhal 2025.1.1      | android.com — bản stable trên Windows  |
| JDK                   | 17                    | Yêu cầu tối thiểu của AGP 8.x         |
| Kotlin                | 2.0.21                | kotlinlang.org — stable                |
| Android Gradle Plugin | 8.7.3                 | dl.google.com — stable                 |
| Gradle                | 8.10.2                | services.gradle.org (ghép với AGP 8.7) |
| Jetpack Compose BOM   | 2024.10.01            | dl.google.com — stable                 |
| compileSdk / targetSdk| API 35 (Android 15)   | android-35 SDK platform                |
| minSdk                | 26 (Android 8.0)      | SPEC mục 2.1                           |
| KSP                   | 2.0.21-1.0.28         | maven.org — khớp 1:1 với Kotlin 2.0.21 |
| Room                  | 2.6.1                 | dl.google.com                          |
| DataStore             | 1.1.1                 | dl.google.com                          |
| Retrofit              | 2.11.0                | squareup                               |
| OkHttp                | 4.12.0                | squareup                               |
| kotlinx.serialization | 1.7.3                 | kotlinlang.org                         |

## Phiên bản thực tế trên máy sau cài đặt

> Điền vào sau khi hoàn tất cài đặt môi trường (cuối phiên T0, cập nhật phiên T1 & A0).

| Công cụ           | Phiên bản cài được | Ngày cài   | Ghi chú                                   |
|-------------------|--------------------|------------|-------------------------------------------|
| PHP               | 8.3.6              | 30/10/2026 | `php -v`                                  |
| Composer          | 2.7.1              | 30/10/2026 | `composer -V`                             |
| Laravel Framework | 13.39.0            | 30/10/2026 | `php artisan --version` (project backend) |
| MySQL             | 8.0.46             | 30/10/2026 | Service active, DB `telesales` & `telesales_test` đã tạo |
| JDK               | OpenJDK 17.0.20.1  | 30/10/2026 | Đã có java, javac, jar                    |
| php-mysql         | pdo_mysql & mysqli | 30/10/2026 | Kết nối PDO MySQL kiểm tra thành công     |
| Android cmdline-tools | 12.0 (11076708) | 30/10/2026 | Cài trong `/home/acer/android-sdk`        |
| Android Platform  | android-35 (API 35)| 30/10/2026 | Cài qua `sdkmanager`                      |
| Android Build-Tools| 35.0.0            | 30/10/2026 | Cài qua `sdkmanager`                      |
| Android Studio    | (cài trên Windows) | —          | Dùng chạy thử GUI trên Windows            |
