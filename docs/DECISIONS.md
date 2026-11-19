# DECISIONS — Quyết định thay đổi / bổ sung so với SPEC gốc

> Ghi mọi quyết định chệch khỏi `docs/TELESALES_SPEC.md` (nguồn chính) hoặc quyết định mặc định trong SPEC.
> Không sửa file này nếu không có lý do rõ ràng và chưa được người dùng duyệt.

---

## D-ARCH-01: Dùng AlarmManager thay WorkManager cho báo thức

- **Quyết định:** Dùng `AlarmManager.setExactAndAllowWhileIdle` + `BroadcastReceiver`, **không** dùng WorkManager.
- **Lý do:** WorkManager không đảm bảo thời điểm chính xác khi máy ở chế độ Doze/tiết kiệm pin — có thể trễ nhiều phút. Ứng dụng telesales cần nhắc đúng phút để không bỏ lỡ cuộc hẹn.
- **Hệ quả:** Cần quyền `SCHEDULE_EXACT_ALARM` (Android 12+); từ Android 14 mặc định bị từ chối — phải kiểm tra `canScheduleExactAlarms()` và hướng dẫn người dùng cấp quyền.
- **Áp dụng ở A8:** Dùng exact alarm khi quyền được cấp; nếu chưa cấp thì dùng báo thức inexact và hiển thị hướng dẫn trong Cài đặt. WorkManager không được dùng cho nhắc đúng phút.
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

## D-A3-01: Điều hướng hồ sơ cũ trong A3 (đã thay bằng màn chi tiết A4)

- **Quyết định ban đầu (A3):** “Mở hồ sơ cũ” trong hộp thoại trùng số mở form sửa khách `existing_customer_id` (GET `/customers/{id}`, cập nhật bằng PATCH). Bấm một khách ở danh sách A2 cũng mở form sửa khách đó.
- **Khi đổi số trùng lúc sửa:** “Mở hồ sơ cũ” thay form hiện tại trong back stack bằng form sửa khách bị trùng; “Sửa lại” đóng hộp thoại và giữ nguyên form hiện tại.
- **Trạng thái sau A4:** Đã thay bằng màn Chi tiết khách. Bấm khách trong danh sách và “Mở hồ sơ cũ” mở chi tiết; form sửa mở từ nút Sửa ở màn chi tiết.
- **Ghi bởi:** Người dùng · 02/11/2026


## D15: Hard delete customers instead of archive

- **Decision:** Permanently delete a customer through `DELETE /api/v1/customers/{id}`. Foreign-key cascades remove interactions, interaction needs, and follow-ups; Android confirms the irreversible action and cancels related local alarms.
- **Reason:** The user wants to fully clean the data and does not need the history of deleted customers.
- **Consequences:** Archive is no longer the deletion behavior; deleted customer lookups return 404. The API returns `deleted_follow_up_ids` so the app can remove local alarms.
- **Recorded by:** User ? 2026-11-03


## D16: Telesales nhanh v? khai th?c nhu c?u kh?ch h?ng

- **Decision:** H? s? kh?ch h?ng l?u `active_since` nullable v? `needs` nullable d??i d?ng JSON array trong c?t TEXT; `industry` hi?n h?u v?n nullable. `needs` ch? nh?n NeedType c? ??nh (website, web_app, software, ads, other). Form ghi nh?n cu?c g?i ch? l?u contacted, ghi ch? v? l?ch g?i l?i t?y ch?n; API m?c ??nh result=contacted v? ch?p nh?n needs r?ng. B?m G?i b?ng ACTION_DIAL t?o Interaction contacted t? ??ng; server g?n contacted_at theo D11.
- **Reason:** Gi?m l?a ch?n b?t bu?c, gi?p telesales nh?p nhanh v? ghi nh?n th?ng tin khai th?c th?nh nhi?u l?a ch?n r? r?ng.
- **Consequences:** Nhu c?u h? s? v? nhu c?u c?a t?ng Interaction l? hai d? li?u ri?ng; notification ph?i c? t?n, s? ?i?n tho?i, ghi ch? tr??c ?? v? m? chi ti?t kh?ch.
- **Recorded by:** User ? 03/11/2026
