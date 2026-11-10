# Hợp đồng API và lỗi

Tất cả endpoint JSON dùng envelope chung:

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "errorCode": null,
  "timestamp": "2026-10-13T10:46:27Z",
  "path": null
}
```

Khi có lỗi, `data` là `null`, `errorCode` là mã ổn định để frontend xử lý và `path` là URI của request:

```json
{
  "success": false,
  "message": "Nội dung JSON không hợp lệ",
  "data": null,
  "errorCode": "MALFORMED_JSON",
  "timestamp": "2026-10-13T10:46:27Z",
  "path": "/api/v1/orders"
}
```

## Mã lỗi nền tảng

| Mã | HTTP status | Ý nghĩa |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Body không đạt Bean Validation |
| `MALFORMED_JSON` | 400 | JSON sai cú pháp hoặc sai kiểu dữ liệu |
| `INVALID_PARAMETER` | 400 | Path/query parameter sai kiểu |
| `INVALID_REQUEST` | 400 | Vi phạm quy tắc nghiệp vụ chung |
| `AUTHENTICATION_FAILED` | 401 | Thiếu hoặc sai thông tin xác thực |
| `TOKEN_EXPIRED` | 401 | JWT hết hạn |
| `ACCESS_DENIED` | 403 | Đã đăng nhập nhưng thiếu quyền |
| `RESOURCE_NOT_FOUND` | 404 | Không tìm thấy dữ liệu nghiệp vụ |
| `ROUTE_NOT_FOUND` | 404 | Không tồn tại endpoint |
| `CONFLICT` | 409 | Trùng dữ liệu hoặc dữ liệu đang được tham chiếu |
| `WORKFLOW_CONFLICT` | 409 | Version, trạng thái hoặc request ID xung đột với quyết định đã lưu |
| `COUPON_INVALID` | 422 | Mã giảm giá hết hạn, chưa đủ điều kiện hoặc không tồn tại |
| `ORDER_LOOKUP_PROOF_REQUIRED` | 403 | Tra cứu đơn khách cần thêm bằng chứng sở hữu |
| `LEGACY_ENDPOINT_DISABLED` | 410 | Endpoint đổi trạng thái tùy ý đã bị khóa và phải thay bằng action API |
| `INTERNAL_ERROR` | 500 | Lỗi ngoài dự kiến; chi tiết chỉ được ghi ở backend |

## Quy ước status

- `200 OK`: đọc hoặc cập nhật thành công.
- `201 Created`: tạo tài khoản, đơn hàng hoặc dữ liệu quản trị thành công.
- `204 No Content`: xóa/ẩn thành công và không có response body.
- `400 Bad Request`: validation, JSON hoặc tham số không hợp lệ.
- `401 Unauthorized`: chưa đăng nhập, token sai hoặc hết hạn.
- `403 Forbidden`: tài khoản không có quyền.
- `404 Not Found`: dữ liệu hoặc endpoint không tồn tại.
- `409 Conflict`: vi phạm unique constraint hoặc quan hệ dữ liệu.
- `410 Gone`: endpoint tương thích cũ đã bị khóa để không thể lách workflow.
- `500 Internal Server Error`: lỗi ngoài dự kiến; response không chứa exception nội bộ.

Validation request body dùng Jakarta Bean Validation trên DTO. Frontend nên hiển thị `message` cho người dùng và dùng `errorCode` cho các nhánh xử lý ổn định, không phân tích nội dung tiếng Việt của `message`.

## Luồng auth và order

Các endpoint dưới đây dùng JWT dạng Bearer khi yêu cầu đăng nhập. API chạy stateless; không dùng session cookie.

| Method | Endpoint | Quyền | Status thành công |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Công khai | `201 Created` |
| `POST` | `/api/v1/auth/login` | Công khai | `200 OK` |
| `POST` | `/api/v1/orders` | Công khai; gắn chủ đơn nếu có JWT | `201 Created` |
| `GET` | `/api/v1/orders/{orderCode}` | Công khai | `200 OK` |
| `GET` | `/api/v1/orders/my-orders` | `USER` hoặc `ADMIN` | `200 OK` |
| `PATCH` | `/api/v1/orders/{orderId}/cancel` | Người dùng đã đăng nhập | `200 OK` |
| `GET` | `/api/v1/orders/admin/all` | `ADMIN` | `200 OK` |
| `GET` | `/api/v1/orders/{orderId}/approval` | Chủ đơn hoặc người có `ORDER_READ` | `200 OK` |
| `GET` | `/api/v1/orders/{orderId}/approval/actions` | Chủ đơn hoặc người có `ORDER_READ` | `200 OK` |
| `GET` | `/api/v1/orders/{orderId}/approval/history` | Chủ đơn hoặc người có `ORDER_READ` | `200 OK` |
| `POST` | `/api/v1/orders/{orderId}/approval/actions/{action}` | Theo `availableActions` | `200 OK` |
| `GET` | `/api/v1/orders/{orderId}/fulfillment/history` | Chủ đơn hoặc người có `ORDER_READ` | `200 OK` |
| `POST` | `/api/v1/orders/{orderId}/fulfillment/actions/{action}` | Người có `ORDER_FULFILL` | `200 OK` |
| `PATCH` | `/api/v1/orders/admin/{orderId}/status` | `ADMIN`; đã ngừng sử dụng | `410 Gone` |

`action` xét duyệt là tên động do `availableApprovalActionMetadata` cung cấp.
Local adapter hiện trả `approve`, `reject` hoặc `withdraw`.
`action` thực hiện đơn nhận `startPreparation`, `dispatch`, `complete` hoặc `cancel`.
Client chỉ hiển thị action có trong `availableApprovalActions` và `availableFulfillmentActions` của response gần nhất.
Mỗi phần tử `availableApprovalActionMetadata` có `name`, `label` và `reasonRequired` để UI không phải biết trước topology của adapter.
Backend luôn kiểm tra lại actor, trạng thái và version khi thực thi.

Mọi action request dùng body sau:

```json
{
  "requestId": "b98e14a9-8f02-4d42-87ca-b7d9e53dc4f7",
  "expectedVersion": 0,
  "reason": "Hết nguyên liệu"
}
```

`requestId` là khóa idempotency ổn định do client sinh cho một ý định.
Gửi lại cùng `requestId` và cùng action trả về kết quả đã lưu mà không lặp hiệu ứng.
Dùng lại `requestId` cho action khác, gửi version cũ hoặc chạy action không còn hợp lệ trả `409 WORKFLOW_CONFLICT`.
`reason` là bắt buộc cho `reject` và `cancel`.

`PATCH /api/v1/orders/{orderId}/cancel` được giữ tạm để client cũ hoạt động nhưng được ánh xạ thành action `withdraw`.
Endpoint này không còn ghi trực tiếp `OrderStatus`.
Endpoint admin nhận trạng thái đích tùy ý luôn trả `410 LEGACY_ENDPOINT_DISABLED`.

Gửi token trong header:

```http
Authorization: Bearer <access-token>
```

Token thiếu, sai hoặc hết hạn trả `401 Unauthorized` với `AUTHENTICATION_FAILED` hoặc `TOKEN_EXPIRED`. Token hợp lệ nhưng không đủ quyền trả `403 Forbidden` với `ACCESS_DENIED`.
## OpenAPI

Swagger UI: `/swagger-ui.html`; tài liệu máy đọc: `/v3/api-docs`. Các endpoint auth dùng `Authorization: Bearer <access-token>`.

Ví dụ response lỗi coupon:

```json
{
  "success": false,
  "errorCode": "COUPON_INVALID",
  "message": "Mã đã hết hạn"
}
```
