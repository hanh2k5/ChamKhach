# Telesales Assistant

Ứng dụng hỗ trợ nhân viên telesales quản lý khách hàng, ghi nhận cuộc gọi và đặt lịch nhắc.

## Cấu trúc

```
telesales-assistant/
├── backend/     # Laravel 13 REST API (PHP 8.3+, MySQL 8.4)
├── android/     # Android app (Kotlin + Jetpack Compose, minSdk 26)
└── docs/        # Đặc tả kỹ thuật, tiến độ, quyết định, phiên bản
```

## Tài liệu

- `docs/SRS.md` — Đặc tả kỹ thuật chi tiết (nguồn sự thật)
- `docs/PROGRESS.md` — Tiến độ hiện tại
- `docs/DECISIONS.md` — Quyết định kiến trúc
- `docs/VERSIONS.md` — Phiên bản đã ghim

## Yêu cầu môi trường

**Backend:** PHP ≥ 8.3.6, Composer ≥ 2.7, MySQL 8.4 LTS  
**Android:** Android Studio Narwhal 2025.1.1 (Windows), JDK 17, Kotlin 2.4.x

## Cài đặt

> Xem `docs/PROGRESS.md` để biết trạng thái hiện tại và lệnh chạy.

### Lưu ý thiết bị Android

Một số máy (Xiaomi, Oppo, Vivo, Samsung bật tiết kiệm pin mạnh) có thể chặn báo thức khi app bị "đóng cứng". Hãy:
1. Tắt tối ưu hóa pin (Battery Optimization) cho app Telesales Assistant.
2. Cho phép chạy nền (Autostart) trong cài đặt hãng.
3. Cấp quyền **Thông báo** và **Báo thức chính xác** khi ứng dụng yêu cầu.
