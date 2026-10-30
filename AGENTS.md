# AGENTS.md — Luật làm việc bắt buộc (đọc đầu MỖI phiên làm việc)

Dự án: Telesales Assistant (Android + Laravel API + MySQL).
Tài liệu chi tiết: `docs/TELESALES_SPEC.md` (nguồn sự thật). SRS gốc: `docs/SRS.md`.
Tiến độ hiện tại: `docs/PROGRESS.md`. Quyết định đã chốt: `docs/DECISIONS.md`.

## 0. Bắt đầu mỗi phiên
1. Đọc: `AGENTS.md` → `docs/PROGRESS.md` → phần của task hiện tại trong `docs/TELESALES_SPEC.md` (mục 12).
2. Nhắc lại bằng 3–5 dòng: task đang làm là gì, tiêu chí xong (DoD) là gì, file nào sẽ đụng tới.
3. Chờ người dùng đồng ý kế hoạch trước khi viết code (với task có sửa >3 file hoặc thêm thư viện).

## 1. Quy tắc phạm vi (chống làm lan man)
- CHỈ làm MỘT task mỗi lần, đúng theo mã task (T1, T2, A1...). Xong thì DỪNG và báo cáo. Không tự làm task kế tiếp.
- KHÔNG làm ngoài SRS: không AI, không ghi âm, không đọc nhật ký cuộc gọi, không đa người dùng, không Zalo/SMS/email.
- KHÔNG thêm thư viện/dependency mới khi chưa hỏi. Danh sách thư viện được phép nằm ở SPEC mục 2.
- KHÔNG refactor, đổi tên, di chuyển file ngoài phạm vi task. Thấy vấn đề ngoài phạm vi → ghi vào `docs/PROGRESS.md` mục "Việc phát hiện thêm", không tự sửa.
- KHÔNG sửa SRS, API contract (SPEC mục 7) hay schema DB đã chốt. Cần đổi → dừng, nêu lý do và đề xuất, chờ duyệt, rồi ghi vào `docs/DECISIONS.md`.
- Gặp điểm chưa rõ: nêu giả định hoặc hỏi lại. Không tự bịa.

## 2. Quy tắc chất lượng code
- Pragmatic SOLID/DRY/KISS: mỗi class một trách nhiệm; không copy-paste logic quá 2 lần; không tạo abstraction khi chưa có 2 nơi dùng.
- Laravel: Controller mỏng → FormRequest (validate) → Service/Action (nghiệp vụ, transaction) → Model → API Resource. Không viết logic nghiệp vụ trong Controller hoặc Model.
- Android: Composable chỉ hiển thị (stateless) → ViewModel (StateFlow<UiState>) → Repository → Retrofit/Room. Không gọi API trong Composable. DTO tách khỏi domain model.
- Thời gian: luôn qua `Clock` có thể tiêm (inject) để test được. Không gọi `now()` rải rác.
- Đặt tên tiếng Anh cho code/DB/API. Chuỗi hiển thị cho người dùng bằng tiếng Việt, đặt trong `strings.xml` (Android).
- Không để code chết, không để `TODO` mà không ghi vào PROGRESS.
- Không hardcode URL, token, mật khẩu. Dùng `.env` (backend) và cấu hình/DataStore (Android). Không log token.

## 3. Quy trình khi viết code (mỗi task)
1. Viết test cho logic nghiệp vụ TRƯỚC hoặc cùng lúc (validation, đếm, quá hạn, lịch).
2. Viết code nhỏ nhất để đạt DoD.
3. CHẠY thật: test + build/lint. Dán output lệnh (đoạn cuối) làm bằng chứng. Không viết "chắc là chạy được".
4. Cập nhật `docs/PROGRESS.md` (xong gì, lệnh để chạy lại, còn gì).
5. Gợi ý commit message; mỗi task = một commit (Conventional Commits: `feat(backend): ...`).
6. Dừng, báo cáo ≤ 10 dòng: đã làm, đã test thế nào, rủi ro, câu hỏi.

## 4. Quy tắc khi gặp lỗi
- Đọc TOÀN BỘ thông báo lỗi và stacktrace; trích dòng lỗi gốc ("Caused by") trước khi sửa.
- Nêu nguyên nhân giả thuyết + cách kiểm chứng, rồi mới sửa. Không sửa mò hàng loạt.
- Mỗi lần chỉ đổi một thứ, chạy lại để xác nhận.
- Tối đa 3 lần thử cho cùng một lỗi. Quá 3 lần → DỪNG, báo cáo: lỗi gì, đã thử gì, kết quả từng lần, đề xuất tiếp theo.
- Tra bảng "Lỗi thường gặp" ở SPEC mục 13 trước.
- Tuyệt đối không: xóa/skip test cho qua, tắt validation, nới lỏng assert, `@Suppress` che lỗi, bắt `Exception` rồi nuốt.
- Không dùng lệnh phá hủy (`migrate:fresh` trên DB thật, `rm -rf`, `git reset --hard`, xóa DB) khi chưa hỏi. `migrate:fresh` chỉ chạy trên DB dev/test và phải nói rõ.

## 5. Bảo toàn ngữ cảnh
- Sau mỗi task, ghi vào PROGRESS: trạng thái, lệnh chạy, quyết định mới, việc tiếp theo.
- Nếu hội thoại dài hoặc bạn không chắc về trạng thái dự án: đọc lại PROGRESS và chạy `git status`, `git log --oneline -10` thay vì đoán.
- Không dựa vào trí nhớ về nội dung file; mở file ra đọc trước khi sửa.

## 6. Định nghĩa "xong" (Definition of Done) chung
- Build/test xanh, có bằng chứng.
- Đáp ứng đúng tiêu chí của task trong SPEC mục 12.
- Không có thay đổi ngoài phạm vi (`git diff --stat` gọn).
- PROGRESS đã cập nhật.
