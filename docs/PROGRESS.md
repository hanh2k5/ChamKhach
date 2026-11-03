# PROGRESS

## Nguồn đặc tả

- `docs/TELESALES_SPEC.md` là nguồn đặc tả chính; `docs/SRS.md` là tài liệu SRS gốc để tham chiếu (D-REPO-01).
- Đã thêm spec đầy đủ (604 dòng) trong commit `8447ecd` (`docs: add detailed spec`).

## Trạng thái

- **Task hiện tại:** A9 hoàn tất code và unit test; `clean testDebugUnitTest assembleDebug` thành công, 72 unit tests xanh trong 18 suites (02/11/2026). Kiểm thử emulator A2–A9 còn nợ. A1 đã được người dùng xác minh build/test và kiểm thử emulator.
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

## A2 — Danh sách khách + tìm kiếm/lọc (hoàn thành)

- Đã thêm DTO/pagination, mapper DTO ↔ domain `Customer`, `ApiCustomerRepository`, và màn danh sách khách làm màn chính với điều hướng Cài đặt.
- ViewModel giữ query, filter và rows; debounce 300 ms, hủy request cũ, reset trang/danh sách khi đổi query/filter, chống gọi tải thêm trùng và ngừng tải khi hết trang.
- Lọc status (mặc định active, archived/all), potential_level và last_result; lấy 20 khách/trang. Thiếu Base URL/token thì không tạo API request và hiện hướng dẫn mở Cài đặt.
- Có component trạng thái loading/empty/error/retry, thông báo 401/mạng/5xx, nhãn tiếng Việt cho kết quả gần nhất, giờ hiển thị theo Asia/Ho_Chi_Minh qua Clock/ZoneId. Có nút làm mới; từ A4 bấm khách mở màn chi tiết.
- Thêm unit tests cho ViewModel (loading/content/empty/error/retry, debounce và kết quả mới nhất, tải thêm chống lặp, reset filter), mapper/múi giờ, và không gọi API khi thiếu cấu hình. Đã xác minh: 19 tests, 0 failures/errors.
- Lệnh xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`.
- API contract và dependency không đổi; navigation-compose đã có từ A0.
- **Còn nợ — thử thủ công A2 trên emulator:** danh sách; tìm kiếm; bộ lọc; lỗi mạng và thử lại; chưa cấu hình Base URL/token; xoay màn hình; tải trang 2.
- Lệnh chạy lại: `cd android && ./gradlew clean testDebugUnitTest assembleDebug`.

## A3 — Form khách hàng + chống trùng (hoàn thành code và unit test)

- Đã thêm form dùng chung cho tạo mới và sửa: chỉ số điện thoại bắt buộc theo API; các trường tên công ty, người liên hệ, địa chỉ, ngành, nguồn, tiềm năng và trạng thái (khi sửa).
- Form sửa nạp GET `/customers/{id}`; tạo bằng POST `/customers`; sửa bằng PATCH `/customers/{id}`. 422 được ánh xạ lỗi theo trường; 409 `DUPLICATE_PHONE` mở dialog với lựa chọn “Mở hồ sơ cũ” hoặc “Sửa lại”; khóa gửi lặp khi đang lưu.
- Theo D-A3-01, mở hồ sơ cũ thay form hiện tại bằng form sửa khách bị trùng; bấm khách trong danh sách mở form sửa; sau khi lưu quay về danh sách và tải lại.
- Unit tests ViewModel với FakeRepository bao phủ tạo/sửa, nạp hồ sơ, 422 theo trường, duplicate ID và chống double-submit; mapper kiểm tra DTO và PATCH gửi null để xóa trường tùy chọn.
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; 25 tests, 0 skipped, 0 failures, 0 errors.
- **Còn nợ — thử thủ công emulator A3:** tạo khách tối thiểu với chỉ SĐT; sửa và xác nhận dữ liệu được nạp/lưu; 422 hiển thị đúng trường; trùng SĐT khi tạo mở đúng form sửa khách cũ; trùng SĐT khi sửa cho phép mở khách trùng hoặc giữ form hiện tại; chặn lưu lặp; quay lại danh sách và thấy dữ liệu mới.

## A4 — Chi tiết khách, timeline, gọi và lưu trữ (hoàn thành code và unit test)

- Đã thêm hồ sơ chi tiết, tổng hợp cuộc gọi/lịch tiếp theo, timeline mới nhất trước với phân trang 20 dòng, trạng thái tải thêm chống lặp, retry và trạng thái timeline rỗng.
- Nút Gọi dùng ACTION_DIAL với số điện thoại; Sửa mở form A3; Lưu trữ xác nhận rồi PATCH status=archived, quay về danh sách và yêu cầu làm mới.
- Khách trong danh sách và “Mở hồ sơ cũ” do hộp thoại trùng số giờ mở màn chi tiết. D-A3-01 đã được cập nhật theo kết quả A4.
- Thời gian API UTC hiển thị theo Asia/Ho_Chi_Minh qua Clock/ZoneId; chuỗi giao diện và nhãn needs/kết quả đặt trong strings.xml.
- Đã thêm unit tests ViewModel với FakeRepository (tải/lỗi, retry, timeline phân trang/chống tải lặp/rỗng, lưu trữ thành công/lỗi) và mapper interaction/UTC.
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; tổng cộng 30 tests, 0 skipped, 0 failures, 0 errors.
- Khi lưu trữ, `cancelled_follow_up_ids` được nhận nhưng chưa đồng bộ hủy báo thức; xử lý thuộc A8.
- **Còn nợ — thử thủ công emulator A2/A3/A4:** mở chi tiết từ danh sách; xem timeline; nút Gọi mở trình quay số đúng số; sửa hồ sơ; lưu trữ có xác nhận; hộp thoại trùng số mở đúng hồ sơ cũ; xoay màn hình. Emulator chưa được thử.
- Lệnh xác minh cần chạy: `cd android && ./gradlew clean testDebugUnitTest assembleDebug`.

## A5 — Form ghi nhận cuộc gọi (hoàn thành code và unit test)

- Từ Chi tiết khách, khách active mở form ghi nhận cuộc gọi; tên/SĐT được tải sẵn, khách archived không có nút ghi nhận. Lưu thành công quay lại Chi tiết và tải mới hồ sơ + timeline.
- Kết quả dùng nút chọn lớn; nhu cầu dùng chip, mặc định `other` và luôn giữ ít nhất một lựa chọn; ghi chú giới hạn 500 ký tự. Có Material3 DatePicker/TimePicker và nút xóa lịch.
- D12 được xử lý bằng hộp thoại “Chọn lịch” / “Lưu không có lịch”. Lịch không ở tương lai bị chặn trước khi gọi mạng bằng Clock tiêm được. Giờ Asia/Ho_Chi_Minh được đổi sang UTC ISO-8601 có `+00:00`; request không chứa `follow_up` khi lịch null.
- POST `/customers/{id}/interactions`; đang lưu khóa nút để chống gửi lặp; lỗi 422 giữ và hiển thị theo trường. Response `follow_up` và `completed_follow_up_ids` hiện chưa đặt/hủy báo thức; phần đó thuộc A8.
- Unit tests A5: 8 tests (kết quả bắt buộc, nhu cầu mặc định/không rỗng, giới hạn ghi chú, D12, chặn lịch quá khứ, chống double-submit, lỗi 422 theo trường, quy đổi qua nửa đêm và DTO bỏ follow_up null). Toàn bộ Android: 38 tests, 0 skipped, 0 failures, 0 errors.
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`.
- **Còn nợ — thử thủ công emulator A2–A5:** A2 danh sách/tìm kiếm/bộ lọc/lỗi mạng-thử lại/chưa cấu hình/xoay màn hình/tải trang 2; A3 tạo/sửa/422/trùng số/chống lưu lặp/quay lại danh sách; A4 mở chi tiết/timeline/Gọi/sửa/lưu trữ/xác nhận/trùng số/xoay màn hình; A5 lưu cuộc gọi không lịch; lưu có lịch; callback không lịch hiện hộp thoại; lịch quá khứ bị chặn; số lần liên hệ tăng đúng 1; timeline có bản ghi mới ở đầu; thời gian hiển thị đúng giờ Việt Nam. Emulator chưa được thử.
- Báo thức chưa được làm; thuộc A8.

## A6 — Màn Lịch cần gọi + thao tác (hoàn thành code và unit test)

- Thêm lối vào “Lịch cần gọi” từ thanh trên màn Danh sách khách. Màn lấy `GET /follow-ups?status=pending&group=all&page={page}&per_page=20`, giữ trạng thái trong ViewModel, tải thêm khi cuộn hết và chặn yêu cầu trang trùng.
- Hiển thị nhóm theo `group` server trả về: quá hạn trước, sau đó hôm nay và sắp tới; thứ tự trong nhóm theo `remind_at` tăng dần. Thời gian hiển thị theo Asia/Ho_Chi_Minh qua Clock/ZoneId tiêm được. Có ghi chú nullable, số khách và nhãn “Quá hạn”.
- Mỗi mục có Gọi (ACTION_DIAL), mở Chi tiết khách, Đã xử lý (PATCH complete) và Dời lịch (Material3 DatePicker/TimePicker). Lịch quá khứ bị chặn client; 409 `FOLLOW_UP_NOT_PENDING` báo và tải lại; 422 hiển thị lỗi `remind_at`; có xử lý lỗi mạng/401/5xx, retry và pull-to-refresh không thêm thư viện.
- Complete loại mục khỏi danh sách; reschedule cập nhật mục theo resource trả về, dùng group server và sắp xếp lại. Khóa các thao tác của mục trong lúc request. Chưa đặt/hủy báo thức: A8 cần cancel khi complete và schedule lại khi reschedule.
- Unit tests A6: 7 test ViewModel cho loading/content/empty/error/retry, thứ tự nhóm, phân trang/chống tải lặp, complete, reschedule + chặn quá khứ, 409 refresh, chống bấm lặp; 1 test mapper kiểm tra note null và UTC → giờ Việt Nam. Toàn bộ Android: 46 tests, 0 skipped, 0 failures, 0 errors (13 suites).
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`.
- **Còn nợ — thử thủ công emulator A2–A6:** A2 danh sách/tìm kiếm/bộ lọc/lỗi mạng-thử lại/chưa cấu hình/xoay màn hình/tải trang 2; A3 tạo/sửa/422/trùng số/chống lưu lặp/quay lại danh sách; A4 mở chi tiết/timeline/Gọi/sửa/lưu trữ/xác nhận/trùng số/xoay màn hình; A5 lưu không lịch/có lịch/D12 callback không lịch/lịch quá khứ/chỉ tăng số liên hệ một lần/timeline mới lên đầu/giờ Việt Nam; A6 mở màn lịch; nhóm quá hạn đứng trước; Gọi mở trình quay số đúng số; Đã xử lý làm mục biến mất; Dời lịch đổi giờ; lịch quá khứ bị chặn; lỗi mạng và thử lại; xoay màn hình. Emulator chưa được thử.

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

## A7 — Dashboard (hoàn thành code và unit test)

- Dashboard gọi `GET /dashboard`; ba thẻ hiển thị trực tiếp `overdue_count`, `today_count`, `potential_count` từ server. Danh sách dùng `items` theo nguyên thứ tự server, tái sử dụng FollowUp mapper/card A6, hiển thị giờ Việt Nam, note tùy chọn, nhãn quá hạn, Gọi và mở Chi tiết.
- Dashboard là màn mở đầu; nút thêm khách, danh sách khách, lịch (qua thẻ Quá hạn/Hôm nay) và Cài đặt có điều hướng. Danh sách khách/lịch cũng có lối truy cập Dashboard/Cài đặt. Dashboard tự refresh khi trở lại màn và có pull-to-refresh Compose không thêm dependency.
- Khi thiếu Base URL/Token, repository dừng trước khi tạo ApiService, UI hướng dẫn mở Cài đặt. Trạng thái loading/error/retry; danh sách rỗng hiện “Hôm nay chưa có lịch nào”, ba thẻ vẫn giữ số liệu server.
- Báo thức chưa được đặt/hủy; tích hợp thuộc A8.
- Unit tests A7: 4 ViewModel tests (loading/content/counts/order, empty, lỗi/retry/refresh số liệu, chưa cấu hình), 2 mapper tests (items rỗng, note null, UTC → giờ Việt Nam, round-trip), 1 repository test xác nhận thiếu cấu hình không tạo API client. Toàn bộ Android: 53 tests, 0 skipped, 0 failures, 0 errors (16 suites).
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`.
- **Còn nợ — thử thủ công A2–A7:** A2–A6 theo checklist phía trên; A7 mở app vào Dashboard; 3 số liệu khớp dữ liệu thực; công việc quá hạn đứng trước; bấm mục mở chi tiết; Gọi mở đúng số; các nút điều hướng; kéo làm mới; lỗi mạng và thử lại; chưa cấu hình Base URL/Token; xoay màn hình. Emulator chưa được thử.

## Việc tiếp theo

1. Thử thủ công A2–A9 trên emulator theo checklist bên dưới.

## A8 — Báo thức và thông báo (hoàn thành code và unit test)

- Thêm Room `reminders` với `ReminderEntity`, `ReminderDao`, `AppDatabase`; không lưu cache khách hàng. `ReminderStore` bọc DAO để test thay được.
- Thêm `ReminderScheduler` và AlarmManager: URI riêng theo follow-up để thay báo thức cùng ID; dùng exact khi quyền cho phép, fallback `setAndAllowWhileIdle` nếu chưa cấp. `ReminderReceiver` xác nhận bản ghi còn trong Room/chưa báo, hiện notification rồi đánh dấu `notified`.
- `NotificationHelper` tạo channel HIGH lúc Application khởi động; notification có giờ Asia/Ho_Chi_Minh, ghi chú, PendingIntent immutable/update-current; mở MainActivity vào hồ sơ khách theo `customer_id`. Không đăng ký quyền/receiver khởi động máy thuộc A9.
- Nối lưu interaction (lịch mới và ID completed), complete/cancel/reschedule, archive; Dashboard và Lịch đồng bộ tất cả trang pending với Room, hủy lịch stale và không đặt lịch quá khứ. ReminderPlanner giữ quyết định thuần, có Clock/ZoneId tiêm được.
- Android 13+ xin quyền thông báo khi vào app; Cài đặt hiển thị trạng thái quyền, cảnh báo, lối tới cài đặt quyền. Bản debug có nút thông báo thử sau 30 giây. WorkManager không dùng vì không bảo đảm đúng phút; quyết định D-ARCH-01.
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; 67 tests, 17 suites, 0 skipped, 0 failures, 0 errors. XML test results ở `android/app/build/test-results/testDebugUnitTest/`.
- **Còn nợ — thử tay trên emulator/điện thoại (chưa thực hiện):** xin quyền thông báo; thông báo debug sau 30 giây kể cả khóa màn hình; đặt lịch thật sau 2 phút và xác nhận giờ bắn; nhấn thông báo mở đúng khách; dời lịch để xác nhận alarm cũ không bắn; hoàn thành/lưu trữ để xác nhận không bắn; tắt thông báo xác nhận app không crash và có giải thích; Android 14+ chưa cấp exact alarm để xác nhận cảnh báo/fallback.

## A9 — Khởi động lại máy và lịch lỡ (hoàn thành code và unit test)

- Thêm `RECEIVE_BOOT_COMPLETED` và `BootReceiver` exported, chỉ nhận `ACTION_BOOT_COMPLETED`. Receiver dùng `goAsync()` với coroutine IO, đọc Room qua `ReminderCoordinator.recoverAfterBoot()`.
- Lịch tương lai chưa báo được schedule lại bằng `ReminderScheduler`; lịch quá hạn chưa báo hiện một notification với chuỗi “Lịch bị lỡ” rồi được đánh dấu notified nguyên tử. Bản ghi rỗng hoặc đã báo không tạo hành động.
- Khi mở app, xử lý Room overdue trước khi gọi đồng bộ API; giữ logic thời gian qua Clock/ZoneId tiêm được. Không dùng WorkManager; không đăng ký `LOCKED_BOOT_COMPLETED`; chưa làm A10.
- Xác minh: `cd android && ./gradlew clean testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; **72 tests, 18 suites**. Lệnh `grep -L 'failures="0" errors="0"' ...` không in file nào. Lệnh đếm test theo yêu cầu dùng `bc` không chạy vì môi trường thiếu `bc`; phép cộng tương đương bằng `awk` trên cùng output `grep` cho kết quả 72.
- **Còn nợ — thử tay (chưa thực hiện):** khởi động lại máy thật/emulator và kiểm tra báo thức tương lai; tắt máy qua giờ hẹn rồi bật lại, kiểm tra thông báo “Lịch bị lỡ”; reboot lần nữa sau khi lịch đã báo để xác nhận không bắn lặp.

## Bugfix D11 — Server tự gán contacted_at (02/11/2026)

- `StoreInteractionRequest` không còn bắt client gửi `contacted_at`; `InteractionService` gán thời điểm nhận request bằng `Carbon::now()` trước khi lưu.
- Thêm feature test POST interaction không có `contacted_at`, xác nhận server lưu và trả về thời điểm hiện tại.
- Android `InteractionCreateDto` tiếp tục không khai báo trường này; unit test xác nhận JSON request không chứa `contacted_at`.
- Xác minh: `cd backend && php artisan test --filter=InteractionTest` — 16 tests, 44 assertions passed; `cd android && ./gradlew.bat testDebugUnitTest` (JBR 21) — `BUILD SUCCESSFUL`.
- Commit: `fix(backend): auto assign contacted_at for interactions (D11)`.

## UI layout fixes — Dashboard, danh sách và chi tiết khách (02/11/2026)

- Dashboard title và các nhãn điều hướng dài chỉ hiển thị một dòng, phần dư được ellipsis; trên màn danh sách chuyển Thêm khách/Lịch cần gọi/Cài đặt xuống NavigationBar.
- Màn danh sách chỉ còn bộ lọc Trạng thái và Tiềm năng; dropdown Kết quả gần nhất được ẩn.
- Nút hành động chi tiết khách chia thành hai hàng, mỗi nút có độ rộng đều và chữ một dòng.
- Xác minh: `cd android && ./gradlew.bat assembleDebug` (JBR 21) — `BUILD SUCCESSFUL`.
- Kiểm tra hồi quy: `clean testDebugUnitTest` với Gradle 8.13 và JBR 21 — `BUILD SUCCESSFUL`; wrapper bị timeout chờ lock nên chạy task bằng distribution đã giải nén.

## UI responsive/glass redesign (03/11/2026)

- Làm mới Material theme với nền gradient trắng/xanh xám, màu xanh dịu, typography thoáng, góc bo pill/20dp và touch target Material tối thiểu 48dp; thêm `GlassCard` và dùng cho thẻ dashboard, danh sách khách, follow-up, hồ sơ và timeline.
- Thêm `WindowSizeClass`: NavigationBar trên compact, NavigationRail trên medium/expanded; danh sách khách và chi tiết chạy dạng hai pane ở màn hình rộng, ViewModel chi tiết được khóa theo customer ID.
- Đổi bộ lọc khách thành hàng `FilterChip` cuộn; thêm `imePadding()` cho form khách, ghi nhận cuộc gọi và Cài đặt; giới hạn bề rộng form để thích ứng tablet/landscape.
- Dependency mới: `androidx.compose.material3:material3-window-size-class:1.3.0`.
- Xác minh: `assembleDebug` với Gradle 8.13/JBR 21 — `BUILD SUCCESSFUL`; emulator đang kết nối nhưng chưa cài/chạy app để kiểm tra xoay màn hình thủ công.

## Bugfix Scaffold và nút xóa khách (03/11/2026)

- `AdaptiveNavigationShell` áp dụng `padding` và `consumeWindowInsets` từ Scaffold cha cho NavHost để tránh nested Scaffold cộng lặp system-bar insets; NavigationBar vẫn nằm ở bottomBar của shell.
- SPEC không có DELETE; nút “Xóa” giữ luồng xác nhận hiện có và gọi `PATCH /customers/{id}` đặt `status=archived`. Thành công làm khách rời danh sách mặc định (active), hủy lịch nhắc qua luồng archive hiện có.
- Cập nhật hộp thoại đúng nội dung được yêu cầu và thông báo thành công.
- Xác minh: `cd android && .\\gradlew.bat assembleDebug` (JBR 21) — `BUILD SUCCESSFUL`.

## Hard delete customer update (2026-11-03)

- Replaced the customer archive flow with authenticated `DELETE /api/v1/customers/{id}`. The API permanently deletes the customer and returns `deleted_follow_up_ids` so Android cancels local alarms.
- Added cascading foreign keys for interactions, interaction needs, and follow-ups; customer listing and interaction recording no longer depend on an archived state.
- Android detail now asks for irreversible deletion confirmation and calls DELETE; removed archived choices from customer filters/forms.
- Updated `TELESALES_SPEC.md` and recorded decision D15 in `DECISIONS.md`.
- Verification: `DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test` passed (102 tests, 280 assertions); `JAVA_HOME=<Android Studio JBR> .\gradlew.bat testDebugUnitTest` passed.
- MySQL is not running locally; the Laravel suite used isolated in-memory SQLite. Run the suite on configured `telesales_test` when MySQL is available.
