# PROGRESS

## Nguồn đặc tả

- `docs/TELESALES_SPEC.md` là nguồn đặc tả chính; `docs/SRS.md` là tài liệu SRS gốc để tham chiếu (D-REPO-01).
- Đã thêm spec đầy đủ (604 dòng) trong commit `8447ecd` (`docs: add detailed spec`).

## Trạng thái

- **10/11/2026 (4):** Thiết kế lại nút bấm và thanh phân trang theo phong cách Apple / Facebook: thay nút chữ Trước/Sau cồng kềnh bằng icon `‹` và `›` 32dp nhỏ gọn bo góc 8dp; bổ sung thanh nhảy trang nhanh trực tiếp `Đến trang [ 95 ] / 100 [ Đi ]` tự động clamp và nhảy trang; thiết kế lại 3 nút hành động `Gọi`, `Xong`, `Dời` trong mỗi thẻ khách hàng theo style iOS/FB compact (chiều cao 34dp, bo góc 8dp, tông màu hiện đại, `Xong` nổi bật). Chạy unit tests xanh, đóng gói và cài đặt thành công lên emulator.
- **10/11/2026 (3):** Sửa triệt để lỗi crash / ANR do vượt hạn mức 500 báo thức của Android (`IllegalStateException: Maximum limit of concurrent alarms 500 reached`): thêm giới hạn `MAX_SCHEDULED_ALARMS = 100` trong `ReminderCoordinator.recoverAfterBoot()`, bọc `try-catch` an toàn trong `AndroidReminderScheduler.schedule()`. Cài đặt và xác nhận trên emulator: app hiển thị và tương tác mượt mà (`mCurrentFocus=MainActivity`).
- **10/11/2026 (2):** Sửa lỗi ứng dụng không phản hồi (ANR) khi mở app với nhiều dữ liệu: chuyển `ReminderCoordinator.sync`, `recoverAfterBoot`, `apply` sang chạy trên `Dispatchers.IO`, tách sync trong `DashboardViewModel` và `FollowUpsViewModel` ra `syncJob` riêng không block UI; giới hạn `MAX_SYNC_PAGES = 5` và `MAX_MISSED_NOTIFICATIONS = 5` chống quá tải IPC AlarmManager/NotificationManager. Unit tests xanh, `assembleDebug` BUILD SUCCESSFUL.
- **10/11/2026:** Nâng cấp UI phân trang thông minh với component `PaginationBar` chung cho cả `FollowUpsScreen` và `CustomersScreen`: luôn neo trang 1 và trang cuối kèm dấu `…`, cho phép nhảy nhanh đến đầu hoặc cuối danh sách khi có nhiều trang (ví dụ 50 trang). Thêm unit test `PaginationBarTest`, chạy toàn bộ test suite và `assembleDebug` BUILD SUCCESSFUL.
- **09/11/2026 (2):** Sửa phân trang FollowUpsScreen bị block — tách `reminderCoordinator.sync()` ra `syncJob` riêng, bỏ guard `listJob?.isActive` trong `goToPage()`. Tests (27 tests) xanh, `assembleDebug` BUILD SUCCESSFUL.
- **09/11/2026:** Căn trái toàn bộ phần Tương tác và Tiềm năng trong màn Chi tiết khách (CustomerDetailScreen), Việt hóa trạng thái 'unrated' thành 'Chưa đánh giá'. Cập nhật seeder DashboardDemoSeeder sinh đúng 1000 lịch quá hạn, 1000 lịch hôm nay và 1000 khách tiềm năng độc lập.
- **06/11/2026:** Sửa Dashboard chart dùng cùng thang đo, hiện số lượng trên đầu mỗi cột; cập nhật seeder để 100 lịch quá hạn, 100 lịch hôm nay và 100 khách tiềm năng luôn có dữ liệu mẫu.
- **06/11/2026:** Sửa nút Tiềm năng trên Dashboard truyền đúng bộ lọc `high,medium`; danh sách tiềm năng nay chỉ hiển thị đúng nhóm và phân trang 20 khách/trang.
- **06/11/2026:** Sửa API lọc danh sách theo `potential`, giữ thứ tự khách mới nhất ở trên (`id DESC`) khi chuyển trang. Thêm test chuyển trang khách tiềm năng.
- **06/11/2026:** Đã căn chỉnh trạng thái lỗi Dashboard và trạng thái kiểm tra trong Cài đặt để nội dung full-width, thẳng hàng và dễ đọc trên màn hình nhỏ. Test Android liên quan thành công.
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
    - Ghi ch? l?ch s? A5: khi tri?n khai ban ??u API y?u c?u ?t nh?t m?t need; quy t?c hi?n t?i ?? ???c thay b?ng ch?p nh?n m?ng r?ng ? workflow m?i b?n d??i.
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
- **Tạo dữ liệu mẫu Dashboard:** `php artisan db:seed --class=DashboardDemoSeeder` (thay dữ liệu mẫu Dashboard bằng 100 quá hạn, 100 hôm nay và 100 khách tiềm năng; không xóa dữ liệu thật)
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

## Delete button stuck-state fix (2026-11-03)

- Customer detail deletion now resets `isDeleting` in `finally`; transient IO errors become a retryable network failure.
- A 404 displays “Khách hàng không còn tồn tại” and closes the detail route. Both dialog actions are disabled only while deletion is in progress.
- Verification: `JAVA_HOME=<Android Studio JBR> .\gradlew.bat assembleDebug`.

## Default customer potential label fix (2026-11-03)

- New customer forms now default to `medium` so the two-choice UI displays the Vietnamese label “Vừa”, not the unsupported `unrated` value.
- Existing stored customer values are not migrated.
- Verification: `JAVA_HOME=<Android Studio JBR> .\gradlew.bat assembleDebug`.

## Source and interaction need display fix (2026-11-03)

- Customer details translate `personal`/`business` source codes to Vietnamese labels.
- Choosing the first concrete need now replaces the default `other`; detail history suppresses a legacy `other` value when a concrete need is also present.
- Verification: `JAVA_HOME=<Android Studio JBR> .\gradlew.bat assembleDebug`.

## Customer UI labels and potential levels (2026-11-03)

- Kept only High/Medium/Low in potential selectors and added exhaustive fallbacks to the label mappings.
- Hid latest call result from customer list/detail summaries while preserving it in the data model and call timeline.
- Verification: `JAVA_HOME=<Android Studio JBR> .\gradlew.bat assembleDebug`.
- Follow-up clarification: selectors now contain only High and Medium; Low is removed from Android UI strings and mappings.

## Customer source dropdown (2026-11-03)

- Replaced the free-text source field with a Material 3 exposed dropdown for `personal` and `business`; the field remains empty until selected.
- Added Vietnamese labels in `strings.xml`. Existing detail `when` mappings already have `else` branches.
- Verification: `JAVA_HOME=<Android Studio JBR> .\gradlew.bat assembleDebug`.


## Streamlined Telesales Workflow (completed 2026-11-03)

- Added nullable `customers.active_since` and JSON-array `customers.needs` stored in TEXT; `industry` was already nullable. Customer API/model/resource support these values.
- Interaction API defaults omitted `result` to `contacted` and accepts empty needs. Android logs an interaction on ACTION_DIAL, streamlines call logging, displays customer need chips, and includes customer name, phone, and note in reminders.
- Verification: `php artisan migrate` succeeded; backend tests passed (104 tests, 292 assertions) using SQLite in-memory. Android `testDebugUnitTest assembleDebug` passed (75 tests). APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Separate customer and company names (2026-11-03)

- Customer create/edit has separate `contact_name` and `company_name` fields. Customer detail renders separate rows and uses the missing-value label when either field is empty.
- Verification: `cd android && .\gradlew.bat assembleDebug` ? `BUILD SUCCESSFUL`.

## Interest marking and label consistency (2026-11-03)

- Added a "Mark Interested" chip to customer detail and call logging. Detail records an `interested` interaction immediately; call logging sends `interested` when selected and retains `contacted` by default.
- Standardized customer identity, phone, address, activity duration, industry, and needs labels through shared Android string resources. Contact name and company name remain separate fields.
- Increased selected needs-chip contrast with the primary color and on-primary text; the required phone field keeps its required hint.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL` (27s).


## Customer potential editing (2026-11-03)

- Added High/Medium selectors to create and edit forms. New customers default to Medium; edits load and preserve the saved choice. Legacy `unrated` values display as Medium and can be changed.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL` (12s).


## Record initial call after customer creation (2026-11-03)

- One Save on a new customer now creates the customer and records one `contacted` interaction with optional future follow-up. The server assigns `contacted_at`; successful follow-ups are scheduled through ReminderCoordinator.
- If interaction recording fails after customer creation, the form retains the created ID and retries only the interaction, preventing a duplicate customer.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL` (12s).


## Vietnamese timestamp display with seconds (2026-11-03)

- Customer last-contact time, customer interaction timeline, and follow-up/dashboard dates now display in Asia/Ho_Chi_Minh using `dd/MM/yyyy HH:mm:ss`. API timestamps remain UTC.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL` (17s).

## Remove new interest marking action (2026-11-03)

- Removed the ability to mark a customer as interested from Customer Detail and Call Log. New call logs always record `contacted`.
- Kept the `interested` result label and display mapping for historical interactions already stored.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Compact customer list cards (2026-11-03)

- Customer list cards now show the customer's name (or company/phone fallback), phone number, and Call action. Address, industry, and last-call time remain available in customer detail.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Compact customer detail layout (2026-11-03)

- Grouped customer fields into compact label/value rows, placed call count and potential side by side, and arranged the four actions in two rows. Needs, follow-up, contact time, deletion confirmation, and call history remain available.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Call schedule defaults to current Vietnam time (2026-11-03)

- The date picker now selects today by default; the time picker starts at the current Vietnam hour and minute instead of a fixed 09:00. Previously selected date/time values are preserved.
- Added a ViewModel test for the Asia/Ho_Chi_Minh date/time conversion using a fixed Clock.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Initial customer call note (2026-11-03)

- Added an optional 500-character note field to the new-customer form. The initial `contacted` interaction and any reminder created with it now carry that note; edit-customer form is unchanged.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## New-customer follow-up time default (2026-11-03)

- Fixed the new-customer form's separate time picker: it now starts at the current hour/minute in Asia/Ho_Chi_Minh instead of 09:00. A previously chosen time remains selected.
- Added a fixed-Clock test for the default local time.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Manual call logging after dialing (2026-11-03)

- Call buttons in customer list and detail now only open ACTION_DIAL. They no longer create a separate automatic Interaction; notes and follow-ups are saved once through Ghi nhận cuộc gọi.
- Updated SPEC behavior and removed unused one-tap recording plumbing.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Compact settings screen (2026-11-03)

- Grouped reminder controls and connection fields into separate GlassCards, reduced page spacing, and placed notification permission and connection actions side by side where supported. Existing status, validation, and permission behavior is unchanged.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Distinct layouts for core screens (2026-11-03)

- Dashboard uses separate semantic colors for KPI tiles and a short task preview; customer list is a flat contact list with compact call actions; follow-ups use a colored date/time badge and clear overdue/today/upcoming treatment.
- Interaction, navigation, call, and reminder actions are unchanged.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Combined Dashboard for customers and follow-ups (2026-11-03)

- Merged follow-up management and the searchable/filterable customer list into the scrolling Dashboard. The main navigation now shows Dashboard and Settings; opening a customer still opens its detail screen.
- Follow-up call/complete/reschedule and customer call/detail actions continue to use their existing ViewModels and repositories. Pull-to-refresh refreshes dashboard metrics, customers, and follow-ups together.
- Updated SPEC section 9.2 to match the unified navigation.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Dashboard chart and focused screen layouts (2026-11-03)

- Added a compact overdue/today bar chart to Dashboard while keeping potential as its own KPI.
- Added visual grouping to customer details, customer form, call logging, and Settings to make each task area easier to scan and reduce the impression of blank pages. No ViewModel/Repository or API behavior changed.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Dashboard as a clean home with a work hub (2026-11-03)

- Removed the customer and follow-up lists from the home Dashboard. Dashboard now contains summary metrics/chart and one Công việc entry point.
- Added a Công việc hub with separate buttons opening the existing customer list and follow-up screens. Existing screen behavior and navigation back paths are preserved.
- Updated SPEC section 9.2 to describe the home/hub structure.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Dashboard chart is the work hub entry point (2026-11-03)

- Removed standalone number tiles, the separate work-hub card, and the add-customer floating button from the home screen.
- The chart now contains overdue, due-today, and potential counts and is the sole work-hub entry point; tapping the chart opens Công việc.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Dashboard direct feature cards (2026-11-04)

- Replaced the intermediate Công việc screen with three independent Dashboard cards.
- Quá hạn opens filtered overdue follow-ups, Hôm nay opens filtered today follow-ups, and Khách tiềm năng opens the high-potential customer list directly.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Dashboard card redesign and potential default filter (2026-11-04)

- Redesigned the three Dashboard entries as separate colored cards with marker, description, count, and direct navigation.
- Fixed the potential-card route so it opens the customer list with the default Tất cả filter, matching the dashboard count of high and medium potential customers.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL`.

## Interactive geography-style dashboard chart (2026-11-04)

- Replaced the three dashboard cards with one compact interactive column chart.
- Added vertical/horizontal axes with arrowheads, dashed guide lines, colored columns, values, and labels.
- Each chart column remains directly clickable for its corresponding overdue, today, or potential screen.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL`.

## Add-customer entry point (2026-11-04)

- Added a visible Thêm khách action to the customer-list top bar.
- Empty customer lists now use Thêm khách as the primary action instead of only retrying.
- The action opens the existing customer form create route; API and form logic are unchanged.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL`.

## Compact Facebook-style UI foundation (2026-11-04)

- Applied a compact visual baseline across the app: smaller interactive minimum size, tighter shared card corners, and reduced card shadows.
- Tightened the Settings screen spacing and status panels while preserving all connection and reminder behavior.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL`.

## Short single-line settings labels (2026-11-04)

- Shortened settings status and action labels so paired controls stay on one line and remain symmetric on narrow screens.
- Existing actions and permission behavior are unchanged.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL`.

## Compact follow-up cards (2026-11-04)

- Simplified follow-up cards by removing repeated group labels, tightening the date badge and spacing, and limiting notes to one line.
- Equalized the three action buttons with short labels: Gọi, Xong, Dời.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug` - `BUILD SUCCESSFUL`.

## Square vertical dashboard bars (2026-11-04)

- Dashboard bars now rise from the baseline with square corners, while tapping the chart still opens Công việc.
- No API, ViewModel, Repository, or navigation behavior changed.
- Verification: `cd android && .\gradlew.bat testDebugUnitTest assembleDebug`.

## Removed customer potential classification (2026-11-04)

- Removed the potential-level field, selectors, customer-list filter, Dashboard potential column, and related navigation from Android.
- Removed `potential_level` from the backend model, validation, resources, dashboard summary, API DTOs, and added a migration to drop the database column.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.
- Backend verification: `cd backend; php artisan test` - 102 tests passed, 276 assertions.

## Restored customer potential classification (2026-11-04)

- Superseded by the later removal requested by the user.

## Vietnamese follow-up date format (2026-11-04)

- Follow-up date buttons now display `dd/MM/yyyy` (for example `04/11/2026`) instead of the ISO format.
- Stored/API date values and scheduling behavior are unchanged.

## Removed customer list potential filter (2026-11-04)

- Superseded by the complete removal below.

## Customer list filter removed, classification retained (2026-11-04)

- Removed only the visible potential filter row from the customer list.
- Restored and retained `potential_level` in the customer form, detail screen, Dashboard, Android API models, Laravel API, and database.
- Migration `2026_11_04_000005_restore_potential_level_for_customer_data.php` restores the data column after the mistaken intermediate removal migration.
- Verification: backend 102 tests passed; Android `testDebugUnitTest assembleDebug` passed.

## Customer list pagination (2026-11-04)

- Customer list now shows 20 customers per page with compact previous, numbered-page, and next controls.
- Replaced automatic load-more scrolling; search and existing customer actions remain unchanged.

## Android scroll performance optimization (2026-11-09)

- Reused Dashboard chart `Paint` and dashed `PathEffect` objects instead of allocating them during each Canvas draw.
- Reduced follow-up row recomposition inputs by passing only each row's busy state and memoized formatted time per follow-up.
- API, ViewModel, Repository, and visible behavior remain unchanged.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Pull-to-refresh scroll smoothness (2026-11-09)

- Prevented the whole screen from recomposing for every pixel while pulling to refresh; the UI now observes only the pulling/not-pulling state.
- Pull-to-refresh behavior and refresh threshold remain unchanged.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Call schedule button spacing (2026-11-09)

- Added inner padding to the call schedule card so the date and time buttons fit neatly within the card boundary.
- Date/time selection behavior remains unchanged.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Follow-up completion flow (2026-11-09)

- The follow-up `Xong` button now opens call recording for the related customer instead of completing the reminder without a call record.
- Saving the call recording creates the interaction and lets the existing backend workflow complete the old pending follow-up; leaving without saving keeps it pending.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Automatic Dashboard refresh after call logging (2026-11-09)

- Saving a call recording now signals the Dashboard to reload its summary when the user returns home.
- The chart updates without requiring a manual pull-to-refresh; API and database contracts remain unchanged.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Dashboard refresh after follow-up navigation (2026-11-09)

- Returning from the overdue/today follow-up screen now signals the Dashboard to reload its summary automatically.
- This keeps the chart current without requiring a manual pull-to-refresh.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Dashboard refresh after potential customer navigation (2026-11-09)

- Returning from the potential customer list now signals the Dashboard to reload its summary automatically.
- Deleting a potential customer is reflected in the Potential chart without a manual pull-to-refresh.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Dashboard demo data expanded (2026-11-09)

- Replaced the previous Dashboard demo records with 200 marked demo customers.
- Verified counts: 100 pending overdue follow-ups, 100 pending today follow-ups, and 100 active high/medium-potential customers.
- Seeder command: `cd backend; php artisan db:seed --class=DashboardDemoSeeder`.

## Dashboard add potential customer button (2026-11-09)

- Restored a `Thêm khách hàng tiềm năng` button below the Dashboard chart.
- The button opens the existing customer form, which defaults to the supported medium potential level; returning to Dashboard refreshes the chart.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew clean testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Removed customer source label (2026-11-09)

- Removed the `Nguồn` line from the customer detail screen.
- The source field remains available in the existing model/API data; only its display was removed.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Settings back button alignment (2026-11-09)

- Changed the Settings back label to `Quay lại` and shifted the button 8dp left so it aligns better with the content edge.
- Navigation behavior is unchanged.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Customer needs compact layout (2026-11-09)

- Displayed `Nhu cầu làm gì` and `Chưa khai thác` on one horizontal row in customer details.
- Multiple needs remain horizontally scrollable without changing stored data.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Compact Dashboard add-customer action (2026-11-09)

- Replaced the full-width potential-customer button with a compact text action aligned to the bottom-right of the Dashboard chart.
- The action still opens the existing customer form and preserves the Dashboard refresh flow.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Boxed Dashboard add-customer action (2026-11-09)

- Added a rounded container and light border around the compact add-potential-customer action.
- The action remains aligned to the chart card's lower-right corner.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Customer detail side spacing (2026-11-09)

- Applied symmetric horizontal content padding to the customer detail list so the profile card and timeline align evenly on both sides.
- No navigation, API, or data behavior changed.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Follow-up action button consistency (2026-11-09)

- Changed the `Dời` follow-up action from a text-only button to an outlined button, matching the outlined `Gọi` action beside it.
- Kept `Xong` as the filled primary action; behavior and navigation are unchanged.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Customer needs alignment (2026-11-09)

- Matched the `Nhu cầu làm gì` row to the same 42/58 label-value columns used by the customer detail rows above.
- The empty value and horizontally scrolling needs chips now start at the same aligned value position.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Notification navigation and debug customer fix (2026-11-09)

- Standardized notification intent handling in `MainActivity` using `pendingCustomerId` and keeping `dashboard` as the base destination, ensuring clean back-stack behavior when returning from customer details.
- Added explicit action, data URI, and `FLAG_ACTIVITY_SINGLE_TOP` in `NotificationHelper` to ensure distinct PendingIntents and reliable delivery to `onNewIntent`.
- Assigned default demo customer ID (1L) to debug reminders in `ResourceReminderCopy` so test notifications open customer details with the call button.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Customer detail 2-column alignment (2026-11-09)

- Aligned the `Nhu cầu làm gì` row perfectly with the 6 rows above by unifying the spacing to `8.dp`, matching typography (`bodySmall` for labels, `bodyMedium` for values), and keeping consistent 42/58 column weights.
- Both columns in the customer profile card are now left-aligned and start at identical horizontal positions across all 7 rows.
- Verification: `wsl.exe bash -lc "cd /mnt/c/Users/Acer/Documents/ChamKhach/android && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew --no-daemon testDebugUnitTest assembleDebug"` - `BUILD SUCCESSFUL`.

## Fast FollowUps refresh after CallLog save (2026-11-20)

- **V?n �?:** Sau khi l�u ghi nh?n cu?c g?i (CallLog), m�n h?nh "L?ch c?n g?i" (FollowUps) t?i l?i ch?m.
- **Nguy�n nh�n:** FollowUpsViewModel.load() g?i eminderCoordinator?.sync(repository) m?i l?n refresh. Sync n�y fetches nhi?u trang t? API v� ghi v�o Room DB, m?t v�i gi�y.
- **Fix:** Th�m flag initialLoaded trong FollowUpsViewModel. Ch? sync l?n �?u m? m�n h?nh; b? qua khi refresh (gi?ng fix �? �p d?ng cho DashboardViewModel).
- **File s?a:** FollowUpsViewModel.kt (th�m flag initialLoaded, wrap sync trong if (!initialLoaded))
- **L?nh ch?y l?i:** $env:JAVA_HOME = "C:\Users\Acer\.jdks\jbr-21.0.11"; cd android; ./gradlew testDebugUnitTest assembleDebug
- **Verification:** BUILD SUCCESSFUL in 55s, APK installed via adb.

## Fast backend Nginx+php-cgi (2026-11-20)
Fix Android: bo refreshDashboard thua trong callLog onSaved.
Fix Backend: Nginx portable + php-cgi 4 workers. 3x nhanh hon artisan serve.
Lenh: powershell -ExecutionPolicy Bypass -File start-backend.ps1

## Fix HTTP Connection Reuse & ApiService Caching (2026-11-20)
- Nguyen nhan delay khi tai lai lan 2: ApiServiceFactory tao moi OkHttpClient cho moi request khien socket bi TIME_WAIT va nghen tren ket noi ao 10.0.2.2.
- Fix: Dung chung ConnectionPool va cache ApiService instance trong ApiServiceFactory.
- Verification: BUILD SUCCESSFUL in 47s, APK installed.

## A10 — Release Packaging & Verification (2026-11-20)
- **Tạo release keystore:** Tạo keystore elease.jks tại ndroid/app/release.jks với key alias 	elesales.
- **Cấu hình signing build.gradle.kts:** Thêm signingConfigs.release, bật isMinifyEnabled = true, isShrinkResources = true.
- **ProGuard rules:** Bổ sung keep rules cho Room entities/DAOs, Retrofit interfaces, OkHttp, DataStore và Kotlinx Serialization DTOs trong proguard-rules.pro.
- **Cấu hình mạng:** Thêm 
etwork_security_config.xml cho phép cleartext với 10.0.2.2 phục vụ test máy ảo và khai báo trong AndroidManifest.xml.
- **Kết quả build:** ./gradlew testDebugUnitTest assembleRelease --no-daemon - **BUILD SUCCESSFUL in 2m 37s**.
- **File APK release:** ndroid/app/build/outputs/apk/release/app-release.apk (2.49 MB).
- **Kiểm chứng chạy thực tế:** Đã gỡ debug build, cài đặt pp-release.apk lên emulator-5554, khởi chạy MainActivity PID 9494 thành công không có crash nào trong logcat. Đã chụp screenshot xác nhận elease_screen.png.
