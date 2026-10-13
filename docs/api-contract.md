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
- `500 Internal Server Error`: lỗi ngoài dự kiến; response không chứa exception nội bộ.

Validation request body dùng Jakarta Bean Validation trên DTO. Frontend nên hiển thị `message` cho người dùng và dùng `errorCode` cho các nhánh xử lý ổn định, không phân tích nội dung tiếng Việt của `message`.
