# DECISIONS — Quyết định thay đổi / bổ sung so với SPEC gốc

> Ghi mọi quyết định chệch khỏi `docs/SRS.md` hoặc quyết định mặc định trong SPEC.
> Không sửa file này nếu không có lý do rõ ràng và chưa được người dùng duyệt.

---

## D-ARCH-01: Dùng AlarmManager thay WorkManager cho báo thức

- **Quyết định:** Dùng `AlarmManager.setExactAndAllowWhileIdle` + `BroadcastReceiver`, **không** dùng WorkManager.
- **Lý do:** WorkManager không đảm bảo thời điểm chính xác khi máy ở chế độ Doze/tiết kiệm pin — có thể trễ nhiều phút. Ứng dụng telesales cần nhắc đúng phút để không bỏ lỡ cuộc hẹn.
- **Hệ quả:** Cần quyền `SCHEDULE_EXACT_ALARM` (Android 12+); từ Android 14 mặc định bị từ chối — phải kiểm tra `canScheduleExactAlarms()` và hướng dẫn người dùng cấp quyền.
- **Ghi bởi:** Agent · T0 · 30/10/2026

---

## D-REPO-01: File docs/TELESALES_SPEC.md không tồn tại

- **Tình huống:** AGENTS.md tham chiếu đến `docs/TELESALES_SPEC.md` nhưng file này không có trong repo. Nội dung đặc tả thực tế nằm trong `docs/SRS.md`.
- **Quyết định:** Mọi phiên làm việc đọc `docs/SRS.md` thay thế. Khi có file `TELESALES_SPEC.md` thực sự sẽ cập nhật lại.
- **Ghi bởi:** Agent · T0 · 30/10/2026

---

## D-ENV-01: Android Studio cài trên Windows, không cài trong WSL2

- **Quyết định:** Android Studio Narwhal 2025.1.1 được cài trên Windows (môi trường GUI), không cài trong WSL2 Ubuntu.
- **Lý do:** Android Studio cần GUI; WSL2 không có màn hình desktop mặc định. Android Gradle build có thể gọi từ WSL2 qua đường dẫn Windows nếu cần.
- **Ghi bởi:** Agent · T0 · 30/10/2026

---

## D-ENV-02: Môi trường thiếu — cần cài thêm trước các task tiếp theo

- **Thiếu trước T1:** MySQL 8.4.7 LTS + PHP extension `pdo_mysql`
- **Thiếu trước A0:** JDK 17, Android Studio Narwhal (Windows)
- **Trạng thái:** Đang tiến hành cài đặt trong phiên T0.
- **Ghi bởi:** Agent · T0 · 30/10/2026
