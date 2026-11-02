# DECISIONS — Quyết định thay đổi / bổ sung so với SPEC gốc

> Ghi mọi quyết định chệch khỏi `docs/TELESALES_SPEC.md` (nguồn chính) hoặc quyết định mặc định trong SPEC.
> Không sửa file này nếu không có lý do rõ ràng và chưa được người dùng duyệt.

---

## D-ARCH-01: Dùng AlarmManager thay WorkManager cho báo thức

- **Quyết định:** Dùng `AlarmManager.setExactAndAllowWhileIdle` + `BroadcastReceiver`, **không** dùng WorkManager.
- **Lý do:** WorkManager không đảm bảo thời điểm chính xác khi máy ở chế độ Doze/tiết kiệm pin — có thể trễ nhiều phút. Ứng dụng telesales cần nhắc đúng phút để không bỏ lỡ cuộc hẹn.
- **Hệ quả:** Cần quyền `SCHEDULE_EXACT_ALARM` (Android 12+); từ Android 14 mặc định bị từ chối — phải kiểm tra `canScheduleExactAlarms()` và hướng dẫn người dùng cấp quyền.
- **Ghi bởi:** Agent · T0 · 30/10/2026

---

## D-REPO-01: Nguồn đặc tả chính

- **Tình huống ban đầu:** `docs/TELESALES_SPEC.md` chưa có trong repo, nên các task trước dùng `docs/SRS.md` thay thế.
- **Quyết định hiện tại:** `docs/TELESALES_SPEC.md` là nguồn đặc tả chính; `docs/SRS.md` là tài liệu SRS gốc để tham chiếu.
- **Cập nhật:** Đã thêm `docs/TELESALES_SPEC.md` trong commit `8447ecd`.
- **Ghi bởi:** Agent · T0 · 30/10/2026; cập nhật 02/11/2026

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

---

## D-A3-01: Mở khách trùng số bằng form sửa trong A3

- **Quyết định:** Trong A3, “Mở hồ sơ cũ” trong hộp thoại trùng số mở form sửa khách `existing_customer_id` (GET `/customers/{id}`, cập nhật bằng PATCH), không mở màn chi tiết A4. Bấm một khách ở danh sách A2 cũng mở form sửa khách đó.
- **Khi đổi số trùng lúc sửa:** “Mở hồ sơ cũ” thay form hiện tại trong back stack bằng form sửa khách bị trùng; “Sửa lại” đóng hộp thoại và giữ nguyên form hiện tại.
- **Sau A4:** Đổi đích bấm khách trong danh sách từ form sửa sang màn chi tiết.
- **Ghi bởi:** Người dùng · 02/11/2026
