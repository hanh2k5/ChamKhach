# API Examples & Contract Freeze (Task T6)

> Contract hiện tại vẫn hỗ trợ phân loại khách tiềm năng trong hồ sơ và Dashboard; chỉ giao diện danh sách Android ẩn bộ lọc tiềm năng.

> **Trạng thái:** ĐÓNG BĂNG CONTRACT (Frozen for Android Client Phase 2).  
> **Ngày phê duyệt:** 30/10/2026.  
> **Nguồn đặc tả gốc:** `docs/SRS.md` mục 7.  
> **Toàn bộ ví dụ dưới đây được thu thập từ lệnh `curl` thực tế chạy trên máy chủ Laravel 13 cục bộ.**

---

## 1. Bảng đối chiếu Contract (SRS Mục 7 vs Thực tế triển khai)

| Endpoint | Method | Trạng thái | Mã HTTP | Đối chiếu SRS Mục 7 | Ghi chú |
|---|---|---|---|---|---|
| `/api/v1/ping` | GET | Khớp 100% | 200, 401 | Mục 7 | Kiểm tra kết nối và tính hợp lệ của token |
| `/api/v1/customers` | GET | Khớp 100% | 200 | Mục 7.1 | Hỗ trợ `q`, `potential`, `last_result`, `status`, phân trang chuẩn Laravel (`data`, `links`, `meta`), sắp xếp `last_contacted_at DESC NULLS LAST, id DESC` |
| `/api/v1/customers` | POST | Khớp 100% | 201, 409, 422 | Mục 7.1 | Bắt buộc `phone`, chống trùng số đã chuẩn hóa (409 `DUPLICATE_PHONE`) |
| `/api/v1/customers/{id}` | GET | Khớp 100% | 200, 404 | Mục 7.1 | Trả đầy đủ trường hồ sơ, `interaction_count`, `last_contacted_at`, `last_result`, `next_follow_up` |
| `/api/v1/customers/{id}` | PATCH | Khớp 100% | 200, 409, 422 | Mục 7.1 | Cập nhật hồ sơ; khi `status: "archived"` tự động hủy pending follow-up và trả `cancelled_follow_up_ids` |
| `/api/v1/customers/{id}/interactions` | GET | Khớp 100% | 200, 404 | Mục 7.2 | Danh sách tương tác phân trang, sắp xếp mới nhất trước |
| `/api/v1/customers/{id}/interactions` | POST | Khớp 100% | 201, 422 | Mục 7.2 | DB Transaction: tạo interaction + needs, D6 tự động complete lịch cũ (`completed_follow_up_ids`), tạo lịch mới nếu có `remind_at`, chặn khách archived (422 `CUSTOMER_ARCHIVED`) |
| `/api/v1/follow-ups` | GET | Khớp 100% | 200 | Mục 7.3 | Lọc theo `group` (overdue, today, upcoming, all) và `status`, sắp xếp `remind_at ASC`, trả kèm `customer` và `note` của interaction tạo lịch |
| `/api/v1/follow-ups/{id}` | PATCH | Khớp 100% | 200, 409, 422 | Mục 7.3 | Máy trạng thái: `complete` (lưu `completed_at`), `cancel`, `reschedule` (remind_at tương lai), chặn 409 `FOLLOW_UP_NOT_PENDING` khi lịch đã kết thúc |
| `/api/v1/dashboard` | GET | Khớp 100% | 200 | Mục 7.4 | Trả `overdue_count`, `today_count`, `potential_count` và tối đa 20 `items` pending |

---

## 2. Quy ước chung cho Client Android
1. **Base URL:** `http://<server-ip>:8000/api/v1` (Emulator: `http://10.0.2.2:8000/api/v1`).
2. **Headers bắt buộc cho mọi request:**
   - `Accept: application/json`
   - `Authorization: Bearer <token>`
   - `Content-Type: application/json` (cho POST / PATCH / PUT)
3. **Múi giờ:**
   - Client gửi và nhận thời gian theo định dạng ISO-8601 có offset (ví dụ `2026-10-30T07:10:00+00:00`).
   - Server phân nhóm `today` theo múi giờ `Asia/Ho_Chi_Minh` (00:00:00 đến 23:59:59.999).
4. **Cấu trúc lỗi chuẩn:**
   - 401 Unauthorized: `{"message": "Unauthenticated."}`
   - 404 Not Found: `{"message": "Not found."}`
   - 409 Duplicate Phone: `{"message": "Số điện thoại đã tồn tại.", "code": "DUPLICATE_PHONE", "existing_customer_id": 12}`
   - 409 Follow-up not pending: `{"message": "Lịch hẹn không ở trạng thái chờ xử lý.", "code": "FOLLOW_UP_NOT_PENDING"}`
   - 422 Validation Error: `{"message": "...", "errors": {"field": ["Chi tiết lỗi"]}}`
   - 422 Archived Customer: `{"message": "Khách hàng đã lưu trữ. Vui lòng kích hoạt lại trước khi ghi nhận cuộc gọi.", "code": "CUSTOMER_ARCHIVED"}`

---

## 3. Chi tiết Request / Response thực tế

### Ping không có token

> **Mô tả:** Từ chối khi thiếu token

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/ping" \
  -H "Accept: application/json"
```

**Response (HTTP 401):**
```json
{
  "message": "Unauthenticated."
}
```

---

### Ping có Bearer token

> **Mô tả:** Xác thực thành công

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/ping" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "status": "ok",
  "message": "pong",
  "user": {
    "id": 1,
    "email": "agent@telesales.local"
  }
}
```

---

### Tạo khách hàng tối thiểu (chỉ có SĐT)

> **Mô tả:** Tự động chuẩn hóa SĐT, các trường khác mang giá trị mặc định

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"phone": "0934417032"}'
```

**Response (HTTP 201):**
```json
{
  "data": {
    "id": 7,
    "company_name": null,
    "contact_name": null,
    "phone": "0934417032",
    "phone_normalized": "0934417032",
    "address": null,
    "industry": null,
    "source": null,
    "potential_level": "unrated",
    "status": "active",
    "interaction_count": 0,
    "last_contacted_at": null,
    "last_result": null,
    "next_follow_up": null,
    "created_at": "2026-10-30T07:09:30+00:00",
    "updated_at": "2026-10-30T07:09:30+00:00"
  }
}
```

---

### Tạo khách hàng đầy đủ trường

> **Mô tả:** Hồ sơ hoàn chỉnh với mức tiềm năng cao

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"company_name": "Công ty TNHH Giải Pháp Số Đại Việt", "contact_name": "Trần Thị Mai", "phone": "+84 944 417 032", "address": "123 Đường Nguyễn Trãi, Quận Thanh Xuân, Hà Nội", "industry": "Bán lẻ", "source": "Giới thiệu", "potential_level": "high"}'
```

**Response (HTTP 201):**
```json
{
  "data": {
    "id": 8,
    "company_name": "Công ty TNHH Giải Pháp Số Đại Việt",
    "contact_name": "Trần Thị Mai",
    "phone": "+84 944 417 032",
    "phone_normalized": "0944417032",
    "address": "123 Đường Nguyễn Trãi, Quận Thanh Xuân, Hà Nội",
    "industry": "Bán lẻ",
    "source": "Giới thiệu",
    "potential_level": "high",
    "status": "active",
    "interaction_count": 0,
    "last_contacted_at": null,
    "last_result": null,
    "next_follow_up": null,
    "created_at": "2026-10-30T07:09:30+00:00",
    "updated_at": "2026-10-30T07:09:30+00:00"
  }
}
```

---

### Tạo khách hàng trùng số điện thoại

> **Mô tả:** Trả về mã lỗi DUPLICATE_PHONE kèm ID khách cũ

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"phone": "0944417032", "company_name": "Công ty Khác"}'
```

**Response (HTTP 409):**
```json
{
  "message": "Số điện thoại đã tồn tại.",
  "code": "DUPLICATE_PHONE",
  "existing_customer_id": 8
}
```

---

### Tạo khách hàng thiếu số điện thoại

> **Mô tả:** Validation lỗi 422

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"company_name": "Không có SĐT"}'
```

**Response (HTTP 422):**
```json
{
  "message": "The phone field is required.",
  "errors": {
    "phone": [
      "The phone field is required."
    ]
  }
}
```

---

### Danh sách khách hàng (phân trang mặc định)

> **Mô tả:** Sắp xếp last_contacted_at DESC NULLS LAST, id DESC

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/customers" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "data": [
    {
      "id": 6,
      "company_name": "Công ty TNHH Giải Pháp Số",
      "contact_name": "Nguyễn Hoàng Nam (Giám Đốc)",
      "phone": "+84 982 666 888",
      "industry": "Công nghệ thông tin",
      "potential_level": "high",
      "status": "active",
      "interaction_count": 2,
      "last_contacted_at": "2026-10-30T07:15:00+00:00",
      "last_result": "interested"
    },
    {
      "id": 4,
      "company_name": "T5 Corp",
      "contact_name": null,
      "phone": "0966888999",
      "industry": null,
      "potential_level": "high",
      "status": "active",
      "interaction_count": 1,
      "last_contacted_at": "2026-10-30T06:00:00+00:00",
      "last_result": "callback"
    },
    {
      "id": 8,
      "company_name": "Công ty TNHH Giải Pháp Số Đại Việt",
      "contact_name": "Trần Thị Mai",
      "phone": "+84 944 417 032",
      "industry": "Bán lẻ",
      "potential_level": "high",
      "status": "active",
      "interaction_count": 0,
      "last_contacted_at": null,
      "last_result": null
    },
    {
      "id": 7,
      "company_name": null,
      "contact_name": null,
      "phone": "0934417032",
      "industry": null,
      "potential_level": "unrated",
      "status": "active",
      "interaction_count": 0,
      "last_contacted_at": null,
      "last_result": null
    },
    {
      "id": 2,
      "company_name": "Công ty Alpha",
      "contact_name": "Chị Mai",
      "phone": "0987 111 222",
      "industry": null,
      "potential_level": "medium",
      "status": "active",
      "interaction_count": 0,
      "last_contacted_at": null,
      "last_result": null
    }
  ],
  "links": {
    "first": "http://127.0.0.1:8007/api/v1/customers?page=1",
    "last": "http://127.0.0.1:8007/api/v1/customers?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "links": [
      {
        "url": null,
        "label": "&laquo; Previous",
        "page": null,
        "active": false
      },
      {
        "url": "http://127.0.0.1:8007/api/v1/customers?page=1",
        "label": "1",
        "page": 1,
        "active": true
      },
      {
        "url": null,
        "label": "Next &raquo;",
        "page": null,
        "active": false
      }
    ],
    "path": "http://127.0.0.1:8007/api/v1/customers",
    "per_page": 20,
    "to": 5,
    "total": 5
  }
}
```

---

### Tìm kiếm và lọc khách hàng

> **Mô tả:** Tìm theo tên công ty và mức tiềm năng

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/customers?q=Đại+Việt&potential=high" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "data": [],
  "links": {
    "first": "http://127.0.0.1:8007/api/v1/customers?page=1",
    "last": "http://127.0.0.1:8007/api/v1/customers?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": null,
    "last_page": 1,
    "links": [
      {
        "url": null,
        "label": "&laquo; Previous",
        "page": null,
        "active": false
      },
      {
        "url": "http://127.0.0.1:8007/api/v1/customers?page=1",
        "label": "1",
        "page": 1,
        "active": true
      },
      {
        "url": null,
        "label": "Next &raquo;",
        "page": null,
        "active": false
      }
    ],
    "path": "http://127.0.0.1:8007/api/v1/customers",
    "per_page": 20,
    "to": null,
    "total": 0
  }
}
```

---

### Chi tiết khách hàng

> **Mô tả:** Bao gồm thông tin tổng hợp và next_follow_up

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/customers/8" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "data": {
    "id": 8,
    "company_name": "Công ty TNHH Giải Pháp Số Đại Việt",
    "contact_name": "Trần Thị Mai",
    "phone": "+84 944 417 032",
    "phone_normalized": "0944417032",
    "address": "123 Đường Nguyễn Trãi, Quận Thanh Xuân, Hà Nội",
    "industry": "Bán lẻ",
    "source": "Giới thiệu",
    "potential_level": "high",
    "status": "active",
    "interaction_count": 0,
    "last_contacted_at": null,
    "last_result": null,
    "next_follow_up": null,
    "created_at": "2026-10-30T07:09:30+00:00",
    "updated_at": "2026-10-30T07:09:30+00:00"
  }
}
```

---

### Chi tiết khách hàng không tồn tại

> **Mô tả:** Trả 404 Not Found

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/customers/999999" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 404):**
```json
{
  "message": "Not found."
}
```

---

### Cập nhật thông tin khách hàng

> **Mô tả:** Cập nhật một phần trường

**Request:**
```bash
curl -X PATCH "http://localhost:8000/api/v1/customers/8" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"contact_name": "Trần Thị Mai (Giám đốc)", "potential_level": "high"}'
```

**Response (HTTP 200):**
```json
{
  "data": {
    "id": 8,
    "company_name": "Công ty TNHH Giải Pháp Số Đại Việt",
    "contact_name": "Trần Thị Mai (Giám đốc)",
    "phone": "+84 944 417 032",
    "phone_normalized": "0944417032",
    "address": "123 Đường Nguyễn Trãi, Quận Thanh Xuân, Hà Nội",
    "industry": "Bán lẻ",
    "source": "Giới thiệu",
    "potential_level": "high",
    "status": "active",
    "interaction_count": 0,
    "last_contacted_at": null,
    "last_result": null,
    "next_follow_up": null,
    "created_at": "2026-10-30T07:09:30+00:00",
    "updated_at": "2026-10-30T07:09:30+00:00"
  }
}
```

---

### Ghi nhận cuộc gọi kèm lịch gọi lại

> **Mô tả:** Tạo interaction, nhu cầu, follow-up pending mới trong 1 transaction

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers/8/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"result": "callback", "needs": ["website", "ads"], "note": "Khách quan tâm gói marketing, hẹn gọi lại chiều thứ Hai", "contacted_at": "2026-10-30T07:10:00+00:00", "follow_up": {"remind_at": "2026-11-03T07:00:00+00:00"}}'
```

**Response (HTTP 201):**
```json
{
  "interaction": {
    "id": 8,
    "result": "callback",
    "needs": [
      "ads",
      "website"
    ],
    "note": "Khách quan tâm gói marketing, hẹn gọi lại chiều thứ Hai",
    "contacted_at": "2026-10-30T07:10:00+00:00"
  },
  "follow_up": {
    "id": 6,
    "customer_id": 8,
    "remind_at": "2026-11-03T07:00:00+00:00",
    "status": "pending"
  },
  "completed_follow_up_ids": [],
  "customer_summary": {
    "interaction_count": 1,
    "last_contacted_at": "2026-10-30T07:10:00+00:00",
    "last_result": "callback"
  }
}
```

---

### Ghi nhận cuộc gọi không có lịch (quy tắc D6)

> **Mô tả:** Lịch pending cũ của khách được tự động hoàn thành (completed_follow_up_ids)

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers/8/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"result": "interested", "needs": ["software"], "note": "Khách chốt dùng thử, hoàn tất lịch cũ", "contacted_at": "2026-10-30T07:15:00+00:00"}'
```

**Response (HTTP 201):**
```json
{
  "interaction": {
    "id": 9,
    "result": "interested",
    "needs": [
      "software"
    ],
    "note": "Khách chốt dùng thử, hoàn tất lịch cũ",
    "contacted_at": "2026-10-30T07:15:00+00:00"
  },
  "follow_up": null,
  "completed_follow_up_ids": [
    6
  ],
  "customer_summary": {
    "interaction_count": 2,
    "last_contacted_at": "2026-10-30T07:15:00+00:00",
    "last_result": "interested"
  }
}
```

---

### Ghi nhận cuộc gọi với needs rỗng

> **Mô tả:** Từ chối 422 theo đặc tả

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers/8/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"result": "callback", "needs": [], "contacted_at": "2026-10-30T07:15:00+00:00"}'
```

**Response (HTTP 422):**
```json
{
  "message": "Nhu cầu là bắt buộc.",
  "errors": {
    "needs": [
      "Nhu cầu là bắt buộc."
    ]
  }
}
```

---

### Danh sách tương tác của khách hàng

> **Mô tả:** Sắp xếp mới nhất trước (contacted_at DESC)

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/customers/8/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "data": [
    {
      "id": 9,
      "result": "interested",
      "needs": [
        "software"
      ],
      "note": "Khách chốt dùng thử, hoàn tất lịch cũ",
      "contacted_at": "2026-10-30T07:15:00+00:00"
    },
    {
      "id": 8,
      "result": "callback",
      "needs": [
        "ads",
        "website"
      ],
      "note": "Khách quan tâm gói marketing, hẹn gọi lại chiều thứ Hai",
      "contacted_at": "2026-10-30T07:10:00+00:00"
    }
  ],
  "links": {
    "first": "http://127.0.0.1:8007/api/v1/customers/8/interactions?page=1",
    "last": "http://127.0.0.1:8007/api/v1/customers/8/interactions?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "links": [
      {
        "url": null,
        "label": "&laquo; Previous",
        "page": null,
        "active": false
      },
      {
        "url": "http://127.0.0.1:8007/api/v1/customers/8/interactions?page=1",
        "label": "1",
        "page": 1,
        "active": true
      },
      {
        "url": null,
        "label": "Next &raquo;",
        "page": null,
        "active": false
      }
    ],
    "path": "http://127.0.0.1:8007/api/v1/customers/8/interactions",
    "per_page": 20,
    "to": 2,
    "total": 2
  }
}
```

---

### Tạo interaction có lịch để test Follow-up

> **Mô tả:** Chuẩn bị lịch cần gọi

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers/7/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"result": "callback", "needs": ["other"], "note": "Gọi lại tư vấn thêm", "contacted_at": "2026-10-30T07:20:00+00:00", "follow_up": {"remind_at": "2026-11-05T08:00:00+00:00"}}'
```

**Response (HTTP 201):**
```json
{
  "interaction": {
    "id": 10,
    "result": "callback",
    "needs": [
      "other"
    ],
    "note": "Gọi lại tư vấn thêm",
    "contacted_at": "2026-10-30T07:20:00+00:00"
  },
  "follow_up": {
    "id": 7,
    "customer_id": 7,
    "remind_at": "2026-11-05T08:00:00+00:00",
    "status": "pending"
  },
  "completed_follow_up_ids": [],
  "customer_summary": {
    "interaction_count": 1,
    "last_contacted_at": "2026-10-30T07:20:00+00:00",
    "last_result": "callback"
  }
}
```

---

### Danh sách lịch cần gọi

> **Mô tả:** Mặc định status=pending, group=all, sắp xếp remind_at tăng dần

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/follow-ups" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "data": [
    {
      "id": 7,
      "remind_at": "2026-11-05T08:00:00+00:00",
      "status": "pending",
      "group": "upcoming",
      "customer": {
        "id": 7,
        "company_name": null,
        "contact_name": null,
        "phone": "0934417032"
      },
      "note": "Gọi lại tư vấn thêm"
    }
  ],
  "links": {
    "first": "http://127.0.0.1:8007/api/v1/follow-ups?page=1",
    "last": "http://127.0.0.1:8007/api/v1/follow-ups?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "links": [
      {
        "url": null,
        "label": "&laquo; Previous",
        "page": null,
        "active": false
      },
      {
        "url": "http://127.0.0.1:8007/api/v1/follow-ups?page=1",
        "label": "1",
        "page": 1,
        "active": true
      },
      {
        "url": null,
        "label": "Next &raquo;",
        "page": null,
        "active": false
      }
    ],
    "path": "http://127.0.0.1:8007/api/v1/follow-ups",
    "per_page": 20,
    "to": 1,
    "total": 1
  }
}
```

---

### Lọc lịch cần gọi theo nhóm upcoming

> **Mô tả:** Chỉ lấy lịch sắp tới sau ngày hôm nay

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/follow-ups?group=upcoming" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "data": [
    {
      "id": 7,
      "remind_at": "2026-11-05T08:00:00+00:00",
      "status": "pending",
      "group": "upcoming",
      "customer": {
        "id": 7,
        "company_name": null,
        "contact_name": null,
        "phone": "0934417032"
      },
      "note": "Gọi lại tư vấn thêm"
    }
  ],
  "links": {
    "first": "http://127.0.0.1:8007/api/v1/follow-ups?page=1",
    "last": "http://127.0.0.1:8007/api/v1/follow-ups?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "links": [
      {
        "url": null,
        "label": "&laquo; Previous",
        "page": null,
        "active": false
      },
      {
        "url": "http://127.0.0.1:8007/api/v1/follow-ups?page=1",
        "label": "1",
        "page": 1,
        "active": true
      },
      {
        "url": null,
        "label": "Next &raquo;",
        "page": null,
        "active": false
      }
    ],
    "path": "http://127.0.0.1:8007/api/v1/follow-ups",
    "per_page": 20,
    "to": 1,
    "total": 1
  }
}
```

---

### Dời lịch gọi lại (Reschedule)

> **Mô tả:** remind_at mới phải ở tương lai

**Request:**
```bash
curl -X PATCH "http://localhost:8000/api/v1/follow-ups/7" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"action": "reschedule", "remind_at": "2026-11-06T09:30:00+00:00"}'
```

**Response (HTTP 200):**
```json
{
  "data": {
    "id": 7,
    "remind_at": "2026-11-06T09:30:00+00:00",
    "status": "pending",
    "group": "upcoming",
    "customer": {
      "id": 7,
      "company_name": null,
      "contact_name": null,
      "phone": "0934417032"
    },
    "note": "Gọi lại tư vấn thêm"
  }
}
```

---

### Hoàn thành lịch gọi lại (Complete)

> **Mô tả:** Cập nhật trạng thái completed và lưu completed_at

**Request:**
```bash
curl -X PATCH "http://localhost:8000/api/v1/follow-ups/7" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"action": "complete"}'
```

**Response (HTTP 200):**
```json
{
  "data": {
    "id": 7,
    "remind_at": "2026-11-06T09:30:00+00:00",
    "status": "completed",
    "group": "upcoming",
    "customer": {
      "id": 7,
      "company_name": null,
      "contact_name": null,
      "phone": "0934417032"
    },
    "note": "Gọi lại tư vấn thêm"
  }
}
```

---

### Thao tác trên lịch đã kết thúc

> **Mô tả:** Trả 409 FOLLOW_UP_NOT_PENDING

**Request:**
```bash
curl -X PATCH "http://localhost:8000/api/v1/follow-ups/7" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"action": "cancel"}'
```

**Response (HTTP 409):**
```json
{
  "message": "Lịch hẹn không ở trạng thái chờ xử lý.",
  "code": "FOLLOW_UP_NOT_PENDING"
}
```

---

### Dữ liệu tổng quan Dashboard

> **Mô tả:** 3 số liệu đếm và danh sách tối đa 20 việc cần gọi

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/dashboard" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response (HTTP 200):**
```json
{
  "overdue_count": 0,
  "today_count": 0,
  "potential_count": 4,
  "items": []
}
```

---

### Tạo thêm lịch trước khi lưu trữ khách

> **Mô tả:** Lịch pending chuẩn bị bị hủy khi archive

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers/7/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"result": "callback", "needs": ["website"], "contacted_at": "2026-10-30T07:22:00+00:00", "follow_up": {"remind_at": "2026-11-09T03:00:00+00:00"}}'
```

**Response (HTTP 201):**
```json
{
  "interaction": {
    "id": 11,
    "result": "callback",
    "needs": [
      "website"
    ],
    "note": null,
    "contacted_at": "2026-10-30T07:22:00+00:00"
  },
  "follow_up": {
    "id": 8,
    "customer_id": 7,
    "remind_at": "2026-11-09T03:00:00+00:00",
    "status": "pending"
  },
  "completed_follow_up_ids": [],
  "customer_summary": {
    "interaction_count": 2,
    "last_contacted_at": "2026-10-30T07:22:00+00:00",
    "last_result": "callback"
  }
}
```

---

### Lưu trữ khách hàng (Archived)

> **Mô tả:** Hủy toàn bộ pending follow-up và trả cancelled_follow_up_ids

**Request:**
```bash
curl -X PATCH "http://localhost:8000/api/v1/customers/7" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "archived"}'
```

**Response (HTTP 200):**
```json
{
  "data": {
    "id": 7,
    "company_name": null,
    "contact_name": null,
    "phone": "0934417032",
    "phone_normalized": "0934417032",
    "address": null,
    "industry": null,
    "source": null,
    "potential_level": "unrated",
    "status": "archived",
    "interaction_count": 2,
    "last_contacted_at": "2026-10-30T07:22:00+00:00",
    "last_result": "callback",
    "next_follow_up": null,
    "created_at": "2026-10-30T07:09:30+00:00",
    "updated_at": "2026-10-30T07:09:30+00:00",
    "cancelled_follow_up_ids": [
      8
    ]
  }
}
```

---

### Ghi nhận cuộc gọi cho khách đã lưu trữ

> **Mô tả:** Trả 422 CUSTOMER_ARCHIVED

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/customers/7/interactions" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"result": "interested", "needs": ["website"], "contacted_at": "2026-10-30T07:25:00+00:00"}'
```

**Response (HTTP 422):**
```json
{
  "message": "Khách hàng đã lưu trữ. Vui lòng kích hoạt lại trước khi ghi nhận cuộc gọi.",
  "code": "CUSTOMER_ARCHIVED"
}
```

---
