# Tích hợp TTCSN với DX-OS v2.0.1

DX-OS chạy độc lập với TTCSN.
TTCSN chỉ gọi gateway DX-OS bằng REST API và OAuth `client_credentials`.

## Điều kiện trước khi chạy TTCSN

- DX-OS `v2.0.1` đã chạy và gateway truy cập được ở cổng `9080`.
- Model `ttcsn_order_approval` đã được áp dụng từ Console hoặc CLI.
- Entity `approval` có các trường `order_id`, `order_code`, `requester_name` và `total_amount`.
- Workflow có các state `pending`, `reviewing`, `accepted`, `rejected` và các action `start_review`, `approve`, `reject`, `withdraw`.
- Hai service client riêng đã được tạo cho vai `REQUESTER` và `APPROVER`.

File YAML không bắt buộc nếu model đã được áp dụng từ Console.

## Cấu hình TTCSN

Đặt các biến sau trong `.env` bị Git bỏ qua:

```dotenv
APPROVAL_PROVIDER=dxos
DXOS_BASE_URL=http://host.docker.internal:9080
DXOS_MODEL=ttcsn_order_approval
DXOS_ENTITY=approval
DXOS_REALM=dxos
DXOS_CONNECT_TIMEOUT=3s
DXOS_READ_TIMEOUT=8s
DXOS_REQUESTER_CLIENT_ID=
DXOS_REQUESTER_CLIENT_SECRET=
DXOS_APPROVER_CLIENT_ID=
DXOS_APPROVER_CLIENT_SECRET=
```

Chỉ điền credential do DX-OS trả về.
Không đặt secret trong source, image, tài liệu hoặc log.

Backend chạy trực tiếp trên host dùng `http://localhost:9080` thay cho `host.docker.internal`.

## Khởi động TTCSN

```powershell
docker compose up -d --build --wait
```

Flyway tự áp dụng V8–V11 trước khi backend nhận request.
Không xóa volume MySQL để né migration.

## Dữ liệu Local đã tồn tại

Sau khi backend chạy với provider `dxos`, quản trị viên gọi preflight:

```text
GET /api/v1/admin/approval-migration/preflight
```

Chỉ khi `issues` rỗng mới chạy:

```text
POST /api/v1/admin/approval-migration
```

Migration giữ reference Local, tạo mapping DX-OS và ghi mốc chuyển đổi vào lịch sử.

## Tiêu chí kiểm tra MVP

1. Tạo order bằng tài khoản customer đã đăng nhập.
2. Xác nhận record tương ứng xuất hiện trong DX-OS ở state `pending`.
3. Dùng approval API hoặc giao diện admin TTCSN thực hiện `start_review` rồi `approve` hoặc `reject`.
4. Xác nhận trạng thái order và lịch sử đồng bộ sau refresh.
5. Restart backend TTCSN và xác nhận mapping vẫn đọc được.

## Giới hạn

MVP dùng hai service identity dùng chung, chưa truyền danh tính từng người dùng TTCSN sang DX-OS.
Production cần TLS, secret manager, retry có giới hạn, outbox hoặc reconciliation, quan sát vận hành và kiểm thử tải.
