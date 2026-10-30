# TELESALES ASSISTANT — ĐẶC TẢ KỸ THUẬT CHI TIẾT (dành cho AI coding agent)

Phiên bản: 1.0 · Ngày: 30/10/2026 · Dựa trên: SRS Telesales Assistant v1.0
Nền tảng: Android (Kotlin + Jetpack Compose) · Backend: Laravel REST API · DB: MySQL
Người dùng: 1 nhân viên telesales (single-user MVP)

> Cách dùng: đặt file này tại `docs/TELESALES_SPEC.md`, đặt SRS gốc tại `docs/SRS.md`, đặt `AGENTS.md` ở thư mục gốc repo. Làm theo đúng thứ tự task ở mục 12, mỗi task một phiên làm việc, xong thì dừng.

---

## 1. Tóm tắt và các quyết định đã chốt

SRS để mở một số câu hỏi. Dưới đây là quyết định mặc định để agent không phải đoán. Người dùng có quyền đổi; đổi thì ghi vào `docs/DECISIONS.md`.

| # | Vấn đề | Quyết định |
|---|--------|-----------|
| D1 | Đăng nhập | Không có màn hình đăng nhập. Backend có 1 user được tạo bằng lệnh artisan; sinh 1 token Sanctum. Người dùng dán `Base URL` + `Token` vào màn Cài đặt của app (một lần). Mọi API yêu cầu `Authorization: Bearer`. |
| D2 | Offline | MVP chỉ online. Không cache dữ liệu khách hàng. Room chỉ dùng cho 1 bảng nhỏ lưu các báo thức đã đặt (phục vụ khởi động lại máy). |
| D3 | Nhắc lại | Nhắc một lần đúng giờ. Không lặp. Người dùng tự dời lịch. |
| D4 | Số điện thoại | Mỗi khách một số chính. **Bắt buộc nhập số điện thoại** (cần để gọi và chống trùng); các trường khác không bắt buộc. Chống trùng theo số đã chuẩn hóa, tính cả khách đã lưu trữ. |
| D5 | Xóa khách | Không xóa cứng. Chỉ có `archived` (ngừng theo dõi). Khi lưu trữ khách: hủy mọi follow-up đang `pending` và hủy báo thức tương ứng. |
| D6 | Mỗi khách có tối đa 1 follow-up `pending` | Khi lưu cuộc gọi mới: (a) follow-up `pending` cũ của khách đó tự chuyển `completed` (vì vừa gọi xong); (b) nếu cuộc gọi có lịch mới thì tạo follow-up `pending` mới. Backend đảm bảo bằng transaction + khóa hàng (`lockForUpdate`) ở service. |
| D7 | "Đã xử lý" = `completed`; "Dời lịch" = cập nhật `remind_at` trên cùng một dòng, trạng thái vẫn `pending`; "Hủy" = `cancelled`. |
| D8 | Nhóm lịch | `overdue` = pending và `remind_at < now`. `today` = pending và `now ≤ remind_at ≤ cuối ngày theo Asia/Ho_Chi_Minh`. `upcoming` = pending và sau cuối ngày hôm nay. |
| D9 | "Khách tiềm năng" (dashboard) | Số khách `status=active` có `potential_level` là `high` hoặc `medium`. Nhãn do người dùng chọn thủ công. |
| D10 | "Trạng thái chăm sóc" để lọc | Lọc theo kết quả cuộc gọi gần nhất (`last_result`), theo `potential_level`, và theo `status` (active/archived). Mặc định danh sách ẩn khách đã lưu trữ. |
| D11 | Ngày giờ gọi (`contacted_at`) | Do server gán bằng thời điểm nhận request (đúng SRS). Không cho client gửi. |
| D12 | Kết quả "Hẹn gọi lại" mà không chọn giờ | App hiện hộp thoại: "Chọn lịch" hoặc "Lưu không có lịch". Backend vẫn chấp nhận (không bắt buộc có lịch). |
| D13 | Thời gian | DB và API dùng UTC. Giao diện hiển thị và "hôm nay" tính theo `Asia/Ho_Chi_Minh`. |
| D14 | Lịch phải ở tương lai | Khi tạo/dời lịch, `remind_at` phải lớn hơn thời điểm hiện tại (backend validate, trả 422). |

---

## 2. Công nghệ và môi trường

### 2.1 Nguyên tắc phiên bản
- Khi khởi tạo dự án (task T0/A0), agent tra phiên bản **ổn định** mới nhất từ trang chính thức, ghi vào `docs/VERSIONS.md`, rồi **ghim cố định** (không dùng `latest`, `+`, `*`, `^` rộng).
- Không dùng bản alpha/beta/RC/dev.
- Mức tối thiểu bắt buộc (nếu bản ổn định mới hơn thì dùng bản mới hơn, nếu tương thích):

| Thành phần | Mức tối thiểu | Ghi chú |
|-----------|---------------|---------|
| PHP | 8.3 | Phải khớp yêu cầu của Laravel đã chọn |
| Laravel | Bản major ổn định mới nhất (≥ 12) | Dùng `composer create-project laravel/laravel` |
| MySQL | 8.4 LTS (hoặc 8.0) | Charset `utf8mb4`, collation `utf8mb4_unicode_ci` |
| Composer | 2.x | |
| Laravel Sanctum | bản đi kèm Laravel | Chỉ dùng personal access token |
| Test backend | PHPUnit hoặc Pest (theo mặc định của Laravel) | Chọn một, không trộn |
| Android Studio | Bản Stable mới nhất | Không dùng Canary/Beta |
| JDK | Theo yêu cầu của Android Gradle Plugin (thường 17 hoặc 21) | Dùng JDK đi kèm Android Studio |
| Kotlin | 2.x | Plugin Compose compiler đi kèm Kotlin 2.x |
| Android Gradle Plugin / Gradle | Cặp tương thích do Android Studio template tạo | Không tự nâng lẻ |
| Jetpack Compose | Qua `compose-bom` | Không ghi version lẻ cho từng thư viện Compose |
| minSdk | 26 (Android 8.0) | Có notification channel sẵn, java.time dùng trực tiếp |
| compileSdk / targetSdk | API ổn định mới nhất lúc khởi tạo | Hành vi alarm/notification phụ thuộc targetSdk |

### 2.2 Môi trường phát triển
- Hệ điều hành: Windows 10/11, macOS hoặc Linux (xác nhận với người dùng ở T0).
- Backend chạy local: Laragon (Windows), Laravel Herd (macOS), hoặc Docker/Laravel Sail. Mặc định gợi ý: Laragon trên Windows. Agent kiểm tra bằng lệnh, không giả định.
- Android: Android Studio + emulator (hoặc điện thoại thật bật USB debugging).
- Git bắt buộc. Repo monorepo.

Lệnh kiểm tra môi trường (T0 phải chạy và dán kết quả):
```
php -v
composer -V
mysql --version
git --version
java -version
```
Android: kiểm tra trong Android Studio (SDK Manager đã cài platform và build-tools đúng `compileSdk`).

### 2.3 Thư viện được phép (ngoài danh sách phải hỏi)

**Backend:** chỉ Laravel + Sanctum. Không thêm package khác (không Spatie, không repository package, không Livewire/Inertia).

**Android (module `app`):**
- Compose: `compose-bom`, `material3`, `ui-tooling-preview`, `ui-tooling` (debug)
- `activity-compose`, `navigation-compose`
- `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`
- `kotlinx-coroutines-android`
- `kotlinx-serialization-json`
- `retrofit`, `converter-kotlinx-serialization` (của Retrofit), `okhttp`, `logging-interceptor` (chỉ debug)
- `room-runtime`, `room-ktx`, `room-compiler` (qua KSP)
- `datastore-preferences`
- Test: `junit`, `kotlinx-coroutines-test`

Không dùng: Hilt/Dagger/Koin (dùng DI thủ công), Moshi/Gson, Glide/Coil (không có ảnh), WorkManager (xem mục 10), thư viện lịch/picker bên thứ ba (dùng Material3 `DatePicker`/`TimePicker`).

---

## 3. Cấu trúc repo

```
telesales-assistant/
├── AGENTS.md
├── docs/
│   ├── SRS.md
│   ├── TELESALES_SPEC.md
│   ├── PROGRESS.md        # agent cập nhật sau mỗi task
│   ├── DECISIONS.md       # quyết định thay đổi so với spec
│   └── VERSIONS.md        # phiên bản đã ghim
├── backend/               # Laravel
│   ├── app/
│   │   ├── Enums/         # CallResult, NeedType, PotentialLevel, CustomerStatus, FollowUpStatus
│   │   ├── Http/
│   │   │   ├── Controllers/Api/
│   │   │   ├── Requests/
│   │   │   └── Resources/
│   │   ├── Models/
│   │   ├── Services/      # CustomerService, InteractionService, FollowUpService, DashboardService
│   │   └── Support/       # PhoneNormalizer, BusinessTime
│   ├── database/migrations/
│   ├── routes/api.php
│   └── tests/{Unit,Feature}/
└── android/
    └── app/src/main/java/<package>/
        ├── TelesalesApp.kt            # Application + AppContainer (DI thủ công)
        ├── MainActivity.kt
        ├── core/                      # time (Clock, formatters), result wrapper, constants
        ├── data/
        │   ├── remote/                # ApiService, dto/, NetworkModule
        │   ├── local/                 # Room: ReminderEntity, ReminderDao, AppDatabase
        │   ├── settings/              # SettingsStore (DataStore)
        │   ├── mapper/
        │   └── repository/            # CustomerRepository, FollowUpRepository, DashboardRepository
        ├── domain/model/              # Customer, Interaction, FollowUp, enums
        ├── reminder/                  # ReminderScheduler, ReminderReceiver, BootReceiver, NotificationHelper
        └── ui/
            ├── navigation/
            ├── theme/
            ├── components/            # nút chọn nhanh, empty/error/loading state dùng chung
            └── screen/{dashboard,customers,customerdetail,customerform,calllog,followups,settings}/
```

---

## 4. Nguyên tắc thiết kế (áp dụng thực dụng)

- **S**ingle responsibility: Controller chỉ nhận request/trả response; Service chứa nghiệp vụ; Resource định dạng đầu ra; Composable chỉ vẽ UI; ViewModel giữ trạng thái; Repository che giấu nguồn dữ liệu.
- **O**pen/closed: enum + mapping tập trung, thêm giá trị mới không phải sửa nhiều chỗ.
- **L/I**: Android định nghĩa interface nhỏ cho thứ cần giả lập trong test (`ReminderScheduler`, repository), không tạo interface cho mọi thứ.
- **D**ependency inversion: ViewModel phụ thuộc interface Repository; `Clock` được tiêm vào.
- **DRY**: một nơi duy nhất cho chuẩn hóa số điện thoại (backend `PhoneNormalizer`), một nơi cho tính thời gian (`BusinessTime`), một bộ component trạng thái loading/empty/error (Android).
- **KISS/YAGNI**: không repository layer ở Laravel, không use-case class ở Android, không multi-module, không offline sync. Chỉ thêm khi có nhu cầu thật.
- Mỗi file ngắn gọn (khuyến nghị < 250 dòng); hàm làm một việc.

---

## 5. Quy tắc nghiệp vụ chi tiết

### 5.1 Enum (giá trị lưu DB/API ở cột trái, nhãn tiếng Việt ở cột phải)

| Enum | Giá trị | Nhãn hiển thị |
|------|---------|---------------|
| CallResult | `interested` | Quan tâm |
| | `no_answer` | Không nghe máy |
| | `callback` | Hẹn gọi lại |
| | `rejected` | Từ chối |
| NeedType | `website` | Website |
| | `web_app` | Web App |
| | `software` | Phần mềm |
| | `ads` | Ads |
| | `other` | Chưa xác định/Khác |
| PotentialLevel | `high` / `medium` / `low` / `unrated` | Cao / Vừa / Thấp / Chưa đánh giá |
| CustomerStatus | `active` / `archived` | Đang theo dõi / Ngừng theo dõi |
| FollowUpStatus | `pending` / `completed` / `cancelled` | — |

### 5.2 Chuẩn hóa số điện thoại (`PhoneNormalizer`)
1. Cắt khoảng trắng; bỏ mọi ký tự không phải số, trừ dấu `+` ở đầu.
2. Nếu bắt đầu bằng `+84` hoặc `84` (và phần còn lại 9–10 số) → thay bằng `0`.
3. Kết quả phải khớp `^0\d{9,10}$`, nếu không → lỗi validation (422) trường `phone`.
4. Lưu `phone` (người dùng nhập, đã trim) và `phone_normalized` (đã chuẩn hóa, UNIQUE).
5. Bắt buộc có unit test: `0912 345 678`, `0912.345.678`, `+84912345678`, `84912345678`, `091234567` (sai), `abc`, chuỗi rỗng.

### 5.3 Thông tin tổng hợp
- `interaction_count` = số bản ghi `interactions` của khách.
- `last_contacted_at` = `MAX(contacted_at)`.
- `last_result` = kết quả của interaction có `contacted_at` lớn nhất (đồng điểm thì `id` lớn nhất).
- **Tính khi truy vấn** (`withCount`, `withMax`, subquery), KHÔNG lưu cột tổng hợp. Cấm N+1 query (có test kiểm tra số query ở danh sách).
- Sửa hồ sơ khách không bao giờ làm đổi/xóa interaction.

### 5.4 Interaction
- Bất biến: không có API sửa/xóa interaction trong MVP.
- `needs`: mảng 0..5 giá trị NeedType, không trùng. Nếu để trống, app mặc định chọn `other` trước khi gửi (backend vẫn chấp nhận mảng rỗng thì lưu rỗng). Quyết định: **backend yêu cầu ít nhất 1 giá trị** (422 nếu rỗng) cho khớp "Chưa xác định/Khác".
- `note`: tối đa 500 ký tự, tùy chọn.

### 5.5 Follow-up — máy trạng thái
```
(create) → pending ──complete──▶ completed
              │──cancel────────▶ cancelled
              └──reschedule────▶ pending (remind_at đổi)
```
- `completed`/`cancelled` là trạng thái cuối: thao tác tiếp theo trả 409 (`code: FOLLOW_UP_NOT_PENDING`).
- Mọi thay đổi trạng thái/giờ do backend quyết định; Android chỉ phản ứng (đặt/hủy báo thức) dựa trên response.
- Quy tắc D6 áp dụng khi lưu interaction mới.

### 5.6 Thời gian (`BusinessTime`)
- `config('app.timezone') = 'UTC'`; thêm `config('telesales.business_timezone') = 'Asia/Ho_Chi_Minh'`.
- API trả thời gian ISO-8601 có offset (ví dụ `2026-10-31T02:00:00+00:00`).
- "Hôm nay" = từ 00:00 đến 23:59:59.999 theo Asia/Ho_Chi_Minh, đổi sang UTC để query.
- Tất cả service nhận `CarbonImmutable $now` hoặc dùng `Carbon::setTestNow` trong test; không gọi `now()` rải rác ngoài `BusinessTime`.

---

## 6. Database (migration)

Charset `utf8mb4`. Tất cả khóa ngoại dùng `restrictOnDelete` (không xóa cascade lịch sử).

**customers**
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | bigint unsigned PK | |
| company_name | varchar(255) null | |
| contact_name | varchar(255) null | |
| phone | varchar(32) | như người dùng nhập |
| phone_normalized | varchar(16) | UNIQUE |
| address | varchar(500) null | |
| industry | varchar(255) null | |
| source | varchar(255) null | |
| potential_level | varchar(16) default `unrated` | enum |
| status | varchar(16) default `active` | enum, index |
| created_at, updated_at | datetime | |

**interactions**
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | bigint PK | |
| customer_id | FK customers | |
| result | varchar(16) | enum |
| note | varchar(500) null | |
| contacted_at | datetime (UTC) | |
| created_at | datetime | |
Index: `(customer_id, contacted_at)`.

**interaction_needs**
| Cột | Kiểu |
|-----|------|
| interaction_id | FK interactions |
| need_type | varchar(16) |
PK/UNIQUE `(interaction_id, need_type)`.

**follow_ups**
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | bigint PK | |
| customer_id | FK | |
| interaction_id | FK null | interaction tạo ra lịch |
| remind_at | datetime (UTC) | |
| status | varchar(16) default `pending` | |
| completed_at | datetime null | |
| created_at, updated_at | datetime | |
Index: `(status, remind_at)`, `(customer_id, status)`.

**users**: dùng bảng mặc định của Laravel (1 dòng). **personal_access_tokens**: của Sanctum.

Seed: Seeder tạo user duy nhất từ biến `.env` (`ADMIN_EMAIL`, `ADMIN_PASSWORD`), không hardcode mật khẩu trong code. Lệnh artisan `telesales:issue-token` in ra token (chỉ in một lần).

---

## 7. API contract (đóng băng sau task T6)

Quy ước chung:
- Prefix `/api/v1`. Mọi request gửi `Accept: application/json`. Mọi API (trừ không có) yêu cầu `Authorization: Bearer <token>`.
- Body JSON, key `snake_case`.
- Phân trang danh sách: `?page=1&per_page=20` (mặc định 20, tối đa 50). Dùng định dạng phân trang chuẩn của Laravel (`data`, `links`, `meta`).
- Lỗi (định dạng thống nhất):
  - 401: `{ "message": "Unauthenticated." }`
  - 404: `{ "message": "Not found." }`
  - 409: `{ "message": "...", "code": "DUPLICATE_PHONE", "existing_customer_id": 12 }` hoặc `code: "FOLLOW_UP_NOT_PENDING"`
  - 422: `{ "message": "...", "errors": { "phone": ["..."] } }`
  - 500: `{ "message": "Server error." }` (không lộ stacktrace; `APP_DEBUG=false` ở production)

### 7.1 Customers

`GET /api/v1/customers?q=&status=active|archived|all&potential=&last_result=&page=&per_page=`
- `q`: tìm theo `company_name`, `contact_name` (LIKE, escape `%` `_`) và `phone_normalized` (nếu `q` chứa số thì chuẩn hóa nhẹ rồi LIKE).
- Sắp xếp: `last_contacted_at` giảm dần (null cuối), rồi `id` giảm dần.
- Response item:
```json
{
  "id": 12,
  "company_name": "Cửa hàng ABC",
  "contact_name": "Chị Lan",
  "phone": "0912 345 678",
  "industry": "Thời trang",
  "potential_level": "medium",
  "status": "active",
  "interaction_count": 3,
  "last_contacted_at": "2026-10-29T03:15:00+00:00",
  "last_result": "interested"
}
```

`POST /api/v1/customers` → 201
```json
{ "company_name": "...", "contact_name": "...", "phone": "0912345678",
  "address": "...", "industry": "...", "source": "...", "potential_level": "unrated" }
```
- Chỉ `phone` bắt buộc. Trùng `phone_normalized` → 409 `DUPLICATE_PHONE` kèm `existing_customer_id`.

`GET /api/v1/customers/{id}` → chi tiết = đầy đủ trường hồ sơ + `interaction_count`, `last_contacted_at`, `last_result`, `next_follow_up` (follow-up pending hoặc null: `{id, remind_at}`).

`PATCH /api/v1/customers/{id}` → cập nhật một phần các trường hồ sơ, `potential_level`, `status`.
- Đổi `phone` trùng người khác → 409.
- `status: "archived"` → hủy mọi follow-up pending của khách (trong cùng transaction) và trả danh sách `cancelled_follow_up_ids` để app hủy báo thức.
- `status: "active"` lại thì không tự tạo lịch.

### 7.2 Interactions

`GET /api/v1/customers/{id}/interactions?page=&per_page=` (mới nhất trước)
```json
{ "id": 55, "result": "callback", "needs": ["website","ads"], "note": "Hẹn thứ 6", "contacted_at": "2026-10-29T03:15:00+00:00" }
```

`POST /api/v1/customers/{id}/interactions` → 201
```json
{
  "result": "callback",
  "needs": ["website", "ads"],
  "note": "Hẹn gọi lại chiều mai",
  "follow_up": { "remind_at": "2026-10-31T07:00:00+00:00" }
}
```
- `follow_up` tùy chọn (bỏ hẳn hoặc `null` nếu không có lịch).
- Khách `archived` → 422 (phải kích hoạt lại trước).
- Response:
```json
{
  "interaction": { "id": 55, "result": "callback", "needs": ["website","ads"], "note": "...", "contacted_at": "..." },
  "follow_up": { "id": 9, "customer_id": 12, "remind_at": "...", "status": "pending" },
  "completed_follow_up_ids": [7],
  "customer_summary": { "interaction_count": 4, "last_contacted_at": "...", "last_result": "callback" }
}
```
- `completed_follow_up_ids`: các lịch cũ bị tự hoàn thành (D6), app dùng để hủy báo thức cũ.
- Tất cả thực hiện trong một DB transaction.

### 7.3 Follow-ups

`GET /api/v1/follow-ups?group=overdue|today|upcoming|all&status=pending&page=&per_page=`
- Mặc định `status=pending`, `group=all`. Sắp xếp `remind_at` tăng dần.
- Item:
```json
{ "id": 9, "remind_at": "...", "status": "pending", "group": "today",
  "customer": { "id": 12, "company_name": "...", "contact_name": "...", "phone": "..." },
  "note": "ghi chú của interaction tạo lịch (hoặc null)" }
```
- Trường `group` do server tính (D8).

`PATCH /api/v1/follow-ups/{id}`
```json
{ "action": "complete" }
{ "action": "cancel" }
{ "action": "reschedule", "remind_at": "2026-11-01T02:00:00+00:00" }
```
- Trả follow-up sau cập nhật. `reschedule` yêu cầu `remind_at` tương lai (D14). Trạng thái không còn `pending` → 409.

### 7.4 Dashboard
`GET /api/v1/dashboard`
```json
{ "overdue_count": 2, "today_count": 5, "potential_count": 8,
  "items": [ /* tối đa 20 follow-up pending: quá hạn trước, sau đó theo remind_at, cùng cấu trúc item follow-up */ ] }
```

### 7.5 Ghi chú bảo mật
- Chỉ HTTPS ở môi trường thật; không log token/số điện thoại ở mức info trên production.
- Rate limit mặc định của Laravel (`throttle:api`) giữ nguyên.

---

## 8. Backend — thiết kế

- Routes: `routes/api.php`, nhóm `v1`, middleware `auth:sanctum`.
- Controller chỉ gọi Service và trả Resource.
- `InteractionService::record(Customer, DTO)`:
  1. Mở transaction.
  2. `lockForUpdate` customer; nếu `archived` → ném exception 422.
  3. Tạo interaction + interaction_needs.
  4. Hoàn thành follow-up pending cũ (nếu có) → ghi lại id.
  5. Nếu có `follow_up.remind_at` → tạo follow-up mới.
  6. Commit; trả kết quả.
- `FollowUpService::apply(FollowUp, action, ?remind_at)` kiểm tra máy trạng thái.
- `CustomerService::archive(Customer)` hủy follow-up pending trong transaction.
- Model dùng `$fillable` tường minh; enum cast; không dùng `$guarded = []`.
- Exception → JSON qua handler trong `bootstrap/app.php` (tùy phiên bản Laravel): đảm bảo API luôn trả JSON kể cả khi thiếu header `Accept`.

---

## 9. Android — thiết kế

### 9.1 Lớp và luồng dữ liệu
`Composable Screen → ViewModel (StateFlow<UiState>) → Repository → ApiService (Retrofit)`.
- `UiState` dạng sealed/data class có: `Loading`, `Content`, `Empty`, `Error(message, canRetry)`.
- Repository trả `Result<T>` (hoặc sealed `AppResult`), map exception mạng thành thông điệp tiếng Việt dễ hiểu: không có mạng, timeout, 401 (token sai → gợi ý vào Cài đặt), 409, 422 (hiện lỗi từng trường), 5xx.
- DTO (`@Serializable`) tách khỏi domain model; mapper rõ ràng. `Json { ignoreUnknownKeys = true }`.
- Thời gian: dùng `java.time` (`Instant`, `ZonedDateTime`, `ZoneId.of("Asia/Ho_Chi_Minh")`). Bọc `Clock` để test.

### 9.2 Màn hình (khớp SRS S-01..S-05 + Cài đặt)
| Màn | Nội dung chính | Ghi chú |
|-----|----------------|---------|
| Dashboard | 3 thẻ (Quá hạn, Hôm nay, Tiềm năng), danh sách việc, nút thêm khách và mở danh sách khách | Pull-to-refresh |
| Danh sách khách | Tên công ty, người liên hệ, SĐT, ngành, ngày gọi gần nhất; ô tìm kiếm (debounce 300ms); bộ lọc | Tải thêm khi cuộn hết (page++) |
| Chi tiết khách | Hồ sơ, tổng hợp, timeline, nút **Gọi** (ACTION_DIAL), nút **Ghi nhận cuộc gọi**, sửa, lưu trữ | Lưu trữ cần xác nhận |
| Form khách | Thêm/sửa; khi gặp 409 hiện hộp thoại "Số này đã có — Mở hồ sơ cũ / Sửa lại" | Chỉ SĐT bắt buộc |
| Ghi nhận cuộc gọi | Tên + SĐT hiển thị sẵn, nút chọn nhanh kết quả (1 lựa chọn), chip nhu cầu (nhiều lựa chọn), ghi chú 1 dòng, chọn ngày giờ gọi lại tùy chọn, nút Lưu lớn | Mục tiêu 10–15 giây; khóa nút Lưu khi đang gửi (chống bấm đúp) |
| Lịch cần gọi | Nhóm Quá hạn / Hôm nay-Sắp tới; mỗi mục có Gọi, Đã xử lý, Dời lịch | |
| Cài đặt | Base URL, Token, kiểm tra kết nối, trạng thái quyền thông báo và báo thức chính xác (nút đi tới cài đặt hệ thống) | |

Tất cả danh sách/chi tiết phải có đủ 4 trạng thái: loading, empty, error, retry (dùng component dùng chung).

### 9.3 Mở trình gọi
`Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone)))` — không cần quyền `CALL_PHONE`. Người dùng tự bấm gọi.

### 9.4 Cấu hình mạng
- Emulator truy cập máy host qua `http://10.0.2.2:<port>`. Điện thoại thật: dùng IP LAN của máy chạy backend (`php artisan serve --host=0.0.0.0`) hoặc `adb reverse tcp:8000 tcp:8000` rồi dùng `http://127.0.0.1:8000`.
- Cleartext (http) chỉ cho bản debug qua `networkSecurityConfig` trong `src/debug`. Bản release bắt buộc https.
- `android:allowBackup="false"` để token không bị sao lưu.

### 9.5 Lưu cài đặt
`SettingsStore` bằng DataStore (Base URL, Token). Không ghi token vào log. Interceptor OkHttp gắn `Authorization` và `Accept`. Khi Base URL đổi, tạo lại Retrofit (đọc URL động qua interceptor hoặc dựng lại client).

---

## 10. Nhắc lịch (Reminder) — thiết kế chi tiết

### 10.1 Chọn giải pháp
- Dùng **`AlarmManager.setExactAndAllowWhileIdle`** + `BroadcastReceiver` để nhắc đúng giờ. **Không** dùng WorkManager cho báo thức đúng phút vì WorkManager không đảm bảo thời điểm chính xác (có thể trễ nhiều phút khi Doze/tiết kiệm pin). Ghi lý do này vào `docs/DECISIONS.md`.
- Quyền trong Manifest:
  - `POST_NOTIFICATIONS` (xin lúc chạy từ Android 13).
  - `SCHEDULE_EXACT_ALARM` (Android 12+). Từ Android 14 quyền này mặc định bị từ chối với app cài mới → phải kiểm tra `alarmManager.canScheduleExactAlarms()`.
  - `RECEIVE_BOOT_COMPLETED`.
  - Không dùng `USE_EXACT_ALARM` (chỉ dành cho app báo thức/lịch theo chính sách Google Play).
- Nếu chưa có quyền báo thức chính xác: dùng `setAndAllowWhileIdle` (không chính xác tuyệt đối) và hiện cảnh báo + nút dẫn tới `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` ở màn Cài đặt.
- Nếu người dùng tắt thông báo: app vẫn lưu lịch bình thường, hiện banner giải thích và nút mở cài đặt thông báo.

### 10.2 Thành phần
- `NotificationHelper`: tạo notification channel (`reminders`, importance HIGH) khi app khởi động; dựng notification (tiêu đề "Gọi lại: <tên khách>", nội dung giờ hẹn + ghi chú, nhấn mở thẳng chi tiết khách).
- `ReminderScheduler` (interface + bản triển khai dùng AlarmManager): `schedule(reminder)`, `cancel(followUpId)`. `requestCode` = `followUpId` (đảm bảo đặt lại cùng id thì thay thế, không nhân đôi). `PendingIntent` dùng `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`.
- `ReminderReceiver`: nhận alarm → kiểm tra trong Room còn tồn tại và chưa `notified` → hiện notification → đánh dấu `notified`.
- `BootReceiver`: sau `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` đọc Room, đặt lại các báo thức tương lai; báo thức đã quá giờ mà chưa `notified` → hiện notification ngay một lần ("Lịch bị lỡ").
- Room `ReminderEntity(followUpId PK, customerId, title, body, remindAtEpochMs, notified)`. Đây là nơi duy nhất app lưu cục bộ.

### 10.3 Khi nào đặt/hủy báo thức
| Sự kiện | Hành động |
|---------|-----------|
| Lưu interaction có follow-up | schedule(follow_up mới); cancel mọi id trong `completed_follow_up_ids` |
| Lưu interaction không có lịch | cancel mọi id trong `completed_follow_up_ids` |
| Hoàn thành/hủy follow-up | cancel(id) |
| Dời lịch | schedule(id, giờ mới) (ghi đè, vẫn một báo thức) |
| Lưu trữ khách | cancel mọi id trong `cancelled_follow_up_ids` |
| Mở app / pull-to-refresh Dashboard | Đồng bộ: lấy `GET /follow-ups?status=pending`; huỷ báo thức không còn pending; đặt báo thức còn thiếu hoặc khác giờ |

### 10.4 Giới hạn phải nói rõ với người dùng
- Một số máy (Xiaomi, Oppo, Vivo, Samsung bật tiết kiệm pin mạnh) có thể chặn báo thức khi app bị "đóng cứng". Hướng dẫn người dùng tắt tối ưu hóa pin và cho phép chạy nền cho app (ghi vào README).
- Nhắc chỉ chính xác khi quyền thông báo và báo thức chính xác được cấp.

---

## 11. Chiến lược kiểm thử

### Backend (PHPUnit/Pest, DB MySQL riêng `telesales_test`, `RefreshDatabase`)
- Unit: `PhoneNormalizer` (nhiều trường hợp), `BusinessTime` (ranh giới 00:00 và 23:59 Asia/Ho_Chi_Minh, đổi sang UTC đúng).
- Feature:
  - Customers: tạo thành công; thiếu phone → 422; trùng phone (kể cả viết khác dạng `+84…`) → 409 + `existing_customer_id`; sửa thành phone trùng → 409; tìm kiếm theo tên và SĐT; lọc; ẩn archived mặc định; lưu trữ hủy follow-up pending.
  - Interactions: tạo → `interaction_count` tăng đúng 1, `last_contacted_at` cập nhật; bản ghi cũ không đổi; thứ tự mới nhất trước; `needs` rỗng → 422; khách archived → 422; `remind_at` quá khứ → 422; tạo mới tự hoàn thành follow-up cũ (D6); lỗi giữa chừng → rollback toàn bộ.
  - Follow-ups: complete/cancel/reschedule; thao tác lên follow-up đã kết thúc → 409; phân nhóm overdue/today/upcoming đúng ở ranh giới (dùng `Carbon::setTestNow`).
  - Dashboard: đếm đúng ba số liệu.
  - Auth: thiếu/sai token → 401 JSON (kể cả thiếu header Accept).
  - Hiệu năng: danh sách khách không N+1 (assert số query cố định).

### Android (unit test JVM)
- ViewModel với Repository giả: trạng thái Loading → Content/Empty/Error; chống bấm Lưu hai lần; xử lý 409 (hiện hộp thoại mở hồ sơ cũ); xử lý 422 hiện lỗi theo trường.
- Logic lập lịch: lớp quyết định hành động báo thức theo bảng 10.3 (hàm thuần, test với `FakeReminderScheduler`).
- Mapper DTO ↔ domain; parse thời gian UTC → Asia/Ho_Chi_Minh.
- Kiểm thử thủ công trên thiết bị/emulator theo checklist mục 15 (báo thức, khởi động lại máy, Doze không thể unit test).

---

## 12. Kế hoạch task (làm TUẦN TỰ, mỗi task một phiên, xong là dừng)

Mỗi task có: **Mục tiêu · Việc làm · DoD (tiêu chí xong)**. Chỉ bắt đầu task sau khi người dùng xác nhận task trước đạt.

### Giai đoạn 0 — Chuẩn bị
**T0. Khởi tạo repo và kiểm tra môi trường**
- Việc làm: tạo cấu trúc thư mục (mục 3), `git init`, `.gitignore` (Laravel + Android), đặt `AGENTS.md` và docs; chạy các lệnh kiểm tra môi trường (2.2); tạo `docs/PROGRESS.md`, `docs/DECISIONS.md`, `docs/VERSIONS.md` (ghi phiên bản thực tế sau khi tra); tạo `docs/READ_ME_FIRST` không cần.
- Không viết code ứng dụng.
- DoD: repo có commit đầu tiên; mọi lệnh môi trường chạy được và kết quả được dán; danh sách thiếu sót (nếu có) được báo.

**T0b. Rà soát SRS** (không code)
- Việc làm: đọc SRS + SPEC, liệt kê điểm mâu thuẫn/chưa rõ còn lại, đề xuất lựa chọn.
- DoD: người dùng duyệt danh sách; quyết định ghi vào DECISIONS.

### Giai đoạn 1 — Backend (xây trước để chốt contract)
**T1. Dựng Laravel + MySQL + migrations + models**
- Việc làm: `composer create-project`, cấu hình `.env` (không commit), timezone UTC, `config/telesales.php`, enum, migrations (mục 6), models + quan hệ.
- DoD: `php artisan migrate` thành công trên DB dev; `php artisan test` chạy (test mẫu); không có route nghiệp vụ.

**T2. Auth token + hạ tầng API**
- Việc làm: Sanctum, seeder user từ `.env`, lệnh `telesales:issue-token`, route group `v1` + `auth:sanctum`, handler lỗi JSON thống nhất, test 401.
- DoD: test 401 xanh; gọi API bằng curl có token trả 200 ở route kiểm thử `/api/v1/ping`.

**T3. PhoneNormalizer + Customers API**
- Việc làm: `PhoneNormalizer`, `CustomerService`, FormRequest, Resource, các endpoint 7.1, test theo mục 11.
- DoD: toàn bộ test Customers xanh, 409 kèm `existing_customer_id` hoạt động, không N+1.

**T4. Interactions API**
- Việc làm: `InteractionService::record` (transaction), endpoint 7.2, quy tắc D6, test.
- DoD: toàn bộ test Interactions xanh (đếm +1, last_contacted_at, rollback, D6).

**T5. Follow-ups + Dashboard API**
- Việc làm: `BusinessTime`, `FollowUpService`, endpoints 7.3 và 7.4, tích hợp lưu trữ khách hủy follow-up.
- DoD: test phân nhóm ở ranh giới thời gian xanh; dashboard đếm đúng.

**T6. Đóng băng API contract**
- Việc làm: chạy toàn bộ test; xuất ví dụ request/response thực tế (curl) vào `docs/API_EXAMPLES.md`; đối chiếu với mục 7; ghi chú khác biệt.
- DoD: người dùng duyệt; từ đây contract đóng băng.

### Giai đoạn 2 — Android
**A0. Dựng dự án Android**
- Việc làm: tạo dự án Compose (Empty Activity), cấu hình `compose-bom`, KSP, kotlinx-serialization, Room, DataStore, theme, `networkSecurityConfig` debug, `allowBackup=false`, `AppContainer`.
- DoD: app chạy trên emulator hiện màn hình trống có điều hướng; `./gradlew assembleDebug` và `./gradlew test` thành công.

**A1. Tầng mạng + Cài đặt**
- Việc làm: `SettingsStore`, `ApiService` (chỉ endpoint cần), interceptor, DTO, mapper cơ bản, màn Cài đặt có nút "Kiểm tra kết nối" gọi `/ping` hoặc `/dashboard`.
- DoD: kết nối được backend local từ emulator (10.0.2.2); báo lỗi đúng khi token sai.

**A2. Danh sách khách + tìm kiếm/lọc**
- Việc làm: `CustomerRepository`, ViewModel, UI danh sách với loading/empty/error/retry, tìm kiếm debounce, lọc, phân trang.
- DoD: danh sách hiển thị dữ liệu thật; test ViewModel xanh.

**A3. Form khách hàng + chống trùng**
- Việc làm: thêm/sửa khách, hiển thị lỗi 422 theo trường, hộp thoại 409 mở hồ sơ cũ.
- DoD: nhập số trùng → hộp thoại, mở được hồ sơ cũ.

**A4. Chi tiết khách + timeline + nút Gọi + lưu trữ**
- Việc làm: màn chi tiết, timeline phân trang, ACTION_DIAL, lưu trữ có xác nhận.
- DoD: bấm Gọi mở ứng dụng Điện thoại với số đã điền; timeline mới nhất trước.

**A5. Form ghi nhận cuộc gọi (chưa có báo thức)**
- Việc làm: kết quả/nhu cầu/ghi chú/lịch tùy chọn (Material3 date/time picker), hộp thoại D12, khóa nút Lưu khi đang gửi, cập nhật chi tiết sau lưu.
- DoD: lưu xong số lần liên hệ tăng đúng 1, thời gian hoàn thành thao tác ≤ 15 giây khi dùng thử.

**A6. Màn Lịch cần gọi + thao tác**
- Việc làm: nhóm quá hạn/hôm nay-sắp tới, Gọi, Đã xử lý, Dời lịch.
- DoD: đổi trạng thái và giờ phản ánh đúng ở backend và UI.

**A7. Dashboard**
- Việc làm: 3 thẻ, danh sách việc, pull-to-refresh, điều hướng.
- DoD: số liệu khớp với `/dashboard`.

**A8. Báo thức và thông báo**
- Việc làm: notification channel, xin quyền `POST_NOTIFICATIONS`, kiểm tra/hướng dẫn quyền báo thức chính xác, `ReminderScheduler`, `ReminderReceiver`, Room `ReminderEntity`, nối với các sự kiện bảng 10.3, đồng bộ khi mở app.
- DoD: đặt lịch sau 2 phút → thông báo hiện đúng giờ; nhấn mở đúng khách; dời/hoàn thành thì báo thức cũ không bắn.

**A9. Khởi động lại máy + trường hợp biên**
- Việc làm: `BootReceiver`, xử lý lịch lỡ, quyền bị tắt (banner), test logic bảng 10.3.
- DoD: khởi động lại emulator → báo thức tương lai vẫn bắn đúng giờ.

**A10. Hoàn thiện và đóng gói**
- Việc làm: rà soát trạng thái loading/empty/error ở mọi màn, chuỗi tiếng Việt trong `strings.xml`, cấu hình release (minify + keep rules cho DTO serializable), icon cơ bản, README hướng dẫn cài và cấu hình pin.
- DoD: `./gradlew assembleRelease` thành công và chạy được; checklist mục 15 đã tích hết.

---

## 13. Lỗi thường gặp và cách xử lý

### Android
| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|-------------|------------------------|-----------|
| `Unable to resolve host` / `CLEARTEXT communication not permitted` | Dùng http trên Android 9+ | Thêm `networkSecurityConfig` chỉ cho debug; release dùng https |
| Emulator gọi `localhost` thất bại | `localhost` là chính emulator | Dùng `10.0.2.2`; điện thoại thật dùng IP LAN hoặc `adb reverse` |
| Backend không nhận kết nối từ điện thoại | `artisan serve` chỉ nghe 127.0.0.1; tường lửa | `php artisan serve --host=0.0.0.0`, mở cổng, cùng Wi-Fi |
| Gradle sync lỗi JDK/AGP | Sai phiên bản JDK | Dùng JDK đi kèm Android Studio; không tự nâng AGP/Kotlin lẻ |
| KSP/Room lỗi "incompatible Kotlin version" | Phiên bản KSP không khớp Kotlin | Dùng phiên bản KSP đúng với phiên bản Kotlin (tiền tố trùng) |
| `SecurityException` khi đặt exact alarm | Chưa có `SCHEDULE_EXACT_ALARM` | Kiểm tra `canScheduleExactAlarms()` trước; fallback inexact + hướng dẫn cấp quyền |
| Không hiện thông báo trên Android 13+ | Chưa xin `POST_NOTIFICATIONS` | Xin quyền lúc chạy, xử lý từ chối |
| Không hiện thông báo trên Android 8+ | Chưa tạo notification channel | Tạo channel khi `Application.onCreate` |
| Crash `PendingIntent` mutability | Thiếu `FLAG_IMMUTABLE` | Luôn dùng `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` |
| Báo thức mất sau khởi động lại | Không đăng ký lại | `BootReceiver` đọc Room và đặt lại |
| Báo thức trễ/không bắn trên máy Xiaomi/Oppo/Vivo | Hãng chặn chạy nền | Hướng dẫn tắt tối ưu pin/autostart; ghi vào README |
| Báo thức bắn đôi hoặc bắn lịch cũ | `requestCode` khác nhau hoặc quên cancel | `requestCode = followUpId`; luôn cancel theo bảng 10.3 |
| Sai giờ 7 tiếng | Nhầm UTC và Asia/Ho_Chi_Minh | Parse bằng `Instant`/`OffsetDateTime`, hiển thị bằng `ZoneId` cố định |
| `MissingFieldException` / lỗi parse JSON | DTO lệch contract | Đối chiếu mục 7; dùng `ignoreUnknownKeys`, field tùy chọn có default `null` |
| Release build parse JSON lỗi | R8 xóa class | Thêm keep rules cho DTO `@Serializable` |
| Mất dữ liệu form khi xoay màn hình | Giữ state trong Composable | Giữ trong ViewModel hoặc `rememberSaveable` |
| Lưu cuộc gọi bị tạo hai bản ghi | Bấm đúp nút Lưu | Khóa nút khi `isSaving`, kiểm tra ở ViewModel |
| Composable gọi API lặp vô hạn | Gọi trong thân composable | Chỉ gọi từ ViewModel (`init`/event) hoặc `LaunchedEffect(key)` |

### Backend
| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|-----------|
| Trả HTML/redirect login thay vì JSON 401 | Thiếu `Accept: application/json` | Interceptor Android luôn gửi; backend ép JSON cho route `api/*` |
| `SQLSTATE[HY000] [2002]` | MySQL chưa chạy / sai host/port | Kiểm tra dịch vụ và `.env` |
| `Access denied for user` | Sai thông tin DB | Sửa `.env`, `php artisan config:clear` |
| Lỗi key quá dài khi migrate | Charset cũ | Dùng `utf8mb4` + MySQL 8; khai báo độ dài cột |
| Sửa `.env` không có tác dụng | Config bị cache | `php artisan config:clear` |
| Sai ngày "hôm nay" | Dùng giờ UTC | Dùng `BusinessTime` (Asia/Ho_Chi_Minh → UTC) |
| Lỗi 500 lộ stacktrace | `APP_DEBUG=true` ở production | Đặt `false`; xem log trong `storage/logs` |
| `MassAssignmentException` | Thiếu `$fillable` | Khai báo tường minh |
| Danh sách chậm/nhiều query | N+1 | `withCount`, `withMax`, subquery; có test đếm query |
| Test dùng nhầm DB dev | Cấu hình test sai | `.env.testing` trỏ `telesales_test`; không bao giờ chạy test trên DB dev |
| Quên phân biệt trùng số khác định dạng | So sánh chuỗi thô | Luôn so `phone_normalized` |
| Tiếng Việt lỗi chữ trong DB | Sai charset | `utf8mb4_unicode_ci` ở DB, connection |

---

## 14. Quy trình làm việc với Antigravity (để agent không làm quá nhiều, không quên ngữ cảnh)

1. **Một task = một phiên trò chuyện mới.** Phiên mới không mang theo trí nhớ → agent phải đọc `AGENTS.md`, `PROGRESS.md` và task hiện tại. Đó là cách chống "quên ngữ cảnh làm sai".
2. **Yêu cầu kế hoạch trước, duyệt rồi mới code.** Dùng chế độ lập kế hoạch/xin duyệt của Antigravity nếu có; nếu không, ép bằng prompt (bên dưới). Với lệnh terminal, bật chế độ hỏi trước khi chạy lệnh nguy hiểm.
3. **Đừng giao nhiều task song song** cho nhiều agent trên cùng thư mục code, sẽ đè file lẫn nhau. Một agent, một task. (Backend T-xx và Android A-xx chỉ chạy song song nếu tách rõ thư mục và contract đã đóng băng ở T6.)
4. **Commit sau mỗi task.** Hỏng thì `git restore`/`git revert` về commit trước, không sửa chồng sửa.
5. **Bằng chứng, không lời hứa:** agent phải dán output của test/build.
6. **Không sửa contract giữa chừng.** Cần đổi → dừng và ghi DECISIONS.

### Mẫu prompt bắt đầu mỗi task (dán nguyên, đổi mã task)
```
Hãy đọc AGENTS.md, docs/PROGRESS.md và mục "12. Kế hoạch task" của docs/TELESALES_SPEC.md.
Chỉ thực hiện task <MÃ_TASK> (ví dụ T3). Không làm task khác.
Bước 1: Tóm tắt task, DoD, danh sách file sẽ tạo/sửa, và các điểm chưa rõ. DỪNG và chờ tôi đồng ý.
Sau khi tôi đồng ý: viết test trước/cùng lúc, code, chạy test/build thật và dán output.
Cuối cùng: cập nhật docs/PROGRESS.md, gợi ý commit message, báo cáo ≤ 10 dòng rồi DỪNG.
```

### Mẫu prompt khi có lỗi
```
Lỗi dưới đây xuất hiện khi chạy <lệnh>. Hãy:
1) Trích dòng lỗi gốc, nêu nguyên nhân giả thuyết và cách kiểm chứng.
2) Tra bảng "Lỗi thường gặp" (mục 13).
3) Sửa MỘT thứ, chạy lại, dán output.
Tối đa 3 lần thử; quá thì dừng và báo cáo đã thử gì. Không xóa/skip test.
<dán toàn bộ log lỗi>
```

### Mẫu prompt khi nghi ngờ agent lạc hướng
```
Dừng. Chạy git status và git diff --stat, liệt kê file đã đổi và giải thích vì sao mỗi file nằm trong phạm vi task <MÃ_TASK>.
File nào ngoài phạm vi thì hoàn tác. Sau đó đọc lại docs/PROGRESS.md và báo cáo trạng thái.
```

### Mẫu `docs/PROGRESS.md`
```
# PROGRESS
## Trạng thái
- Task hiện tại: T?
- Đã xong: (danh sách, kèm commit hash)
## Cách chạy lại
- Backend: <lệnh>
- Test backend: <lệnh>
- Android build/test: <lệnh>
## Quyết định mới (xem thêm DECISIONS.md)
## Việc phát hiện thêm (ngoài phạm vi, chưa làm)
## Vấn đề đang mở / câu hỏi
```

---

## 15. Checklist nghiệm thu (đối chiếu SRS mục 11)

- [ ] Tạo khách với SĐT + thông tin cơ bản; tìm lại bằng tên/SĐT. (T3, A2, A3)
- [ ] Nhập SĐT đã có (kể cả khác định dạng) → cảnh báo và mở hồ sơ cũ. (T3, A3)
- [ ] Ghi nhận cuộc gọi với kết quả + nhu cầu; ghi chú và lịch để trống được. (T4, A5)
- [ ] Sau lưu: số lần liên hệ +1 đúng; ngày gọi gần nhất cập nhật. (T4, A5)
- [ ] Lịch sử mới nhất trước, bản ghi cũ giữ nguyên. (T4, A4)
- [ ] Đặt lịch tương lai → thông báo đúng ngày giờ (khi đã cấp quyền). (A8)
- [ ] Dời/hoàn thành/hủy lịch → báo thức cũ không bắn. (T5, A6, A8)
- [ ] Dashboard phân loại đúng quá hạn / hôm nay. (T5, A7)
- [ ] Đóng/mở app không mất dữ liệu đã đồng bộ. (A2)
- [ ] Mở ứng dụng Điện thoại với số khách từ chi tiết và Lịch cần gọi. (A4, A6)
- [ ] Khởi động lại máy → báo thức tương lai vẫn hoạt động. (A9)
- [ ] Không có tính năng ngoài phạm vi (AI, ghi âm, đọc nhật ký cuộc gọi, đa người dùng).

---

## 16. Hàng rào phạm vi (để agent không tự mở rộng)

Không làm trong MVP (trừ khi người dùng yêu cầu bằng văn bản): AI/chatbot, tự nhận diện cuộc gọi, đọc nhật ký cuộc gọi, ghi âm, chuyển giọng nói thành văn bản, đồng bộ nhiều nhân viên/phân quyền, Zalo/SMS/email/tổng đài, báo cáo doanh thu/KPI, offline sync, nhiều số điện thoại/khách, sửa/xóa interaction, nhắc lặp lại, widget, dark-mode tinh chỉnh, đa ngôn ngữ.
