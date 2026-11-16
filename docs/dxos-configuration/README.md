# Tích hợp TTCSN với DX-OS

Khách hàng chỉ cần khai báo model, điền cấu hình môi trường và chạy một lệnh.
Script setup tự tải bản phát hành DX-OS đã ghim, kiểm tra checksum, triển khai toàn bộ stack nền tảng, apply model, cấp hai service client, khởi động TTCSN và chờ các container healthy.
Khách hàng không cần mở Keycloak, tạo user kỹ thuật hoặc gán role thủ công.
Khách hàng cũng không cần clone repository hoặc cài sẵn DX-OS CLI.
Bộ cài đang ghim [DX-OS v2.0.0](https://github.com/HaUI-ImplSolution/DX-OS/releases/tag/v2.0.0), là release mới nhất được kiểm tra ngày 2026-11-16.

Máy khách hàng cần Docker Desktop với Compose v2, Git for Windows và kết nối tới GitHub/GitHub Container Registry.
Không cần Java, Maven hay source code DX-OS để dựng stack DX-OS.

## 1. Khách hàng khai báo model

Khách hàng tự tạo `dxos.yaml` tại thư mục gốc dự án.
Có thể tham khảo [`dxos.example.yaml`](../../dxos.example.yaml), nhưng installer không sao chép hoặc tự quyết định model thay khách hàng.
Tên model và entity phải khớp với `DXOS_MODEL` và `DXOS_ENTITY` trong `.env`.

Contract TTCSN yêu cầu model có hai role:

- `REQUESTER` tạo và đọc yêu cầu của mình.
- `APPROVER` đọc và xử lý mọi yêu cầu.

Workflow mẫu là `pending → reviewing → accepted/rejected`.
Các field `order_id`, `order_code`, `requester_name`, `total_amount` và state chuẩn hóa trong file tham khảo là contract hiện tại của adapter TTCSN.

## 2. Điền `.env`

Tạo file cấu hình một lần:

```powershell
Copy-Item .env.dxos.example .env
```

Điền tối thiểu:

```dotenv
JWT_SECRET=<base64-secret-co-it-nhat-32-byte>
MYSQL_PASSWORD=<mat-khau-mysql-ung-dung>
MYSQL_ROOT_PASSWORD=<mat-khau-root-mysql>
DXOS_ACCEPT_LEVEL=SAFE
DXOS_APP_NAME=<ten-tich-hop-cua-khach-hang>
DXOS_HOST_PORT=19080
DXOS_POSTGRES_HOST_PORT=127.0.0.1:55432
DXOS_MODEL=<name-trong-dxos.yaml>
DXOS_ENTITY=<entity-trong-dxos.yaml>
```

`DXOS_HOST_PORT` là cổng gateway khách hàng muốn công bố trên máy.
`DXOS_POSTGRES_HOST_PORT` chỉ bind PostgreSQL DX-OS vào loopback để tránh đụng PostgreSQL khác.
Release, URL tải, checksum, thư mục runtime và URL nội bộ từ TTCSN sang DX-OS do installer quản lý, không phải cấu hình khách hàng.
Installer dừng ngay nếu khách hàng chưa tạo `dxos.yaml`, để trống trường bắt buộc hoặc vẫn giữ tên placeholder của file tham khảo.

Không điền các biến được đánh dấu tự động.
Script tự xác định `DXOS_BASE_URL`, cấp credential và ghi vào `.env` mà không in secret ra màn hình:

```dotenv
DXOS_BASE_URL=
DXOS_REQUESTER_CLIENT_ID=
DXOS_REQUESTER_CLIENT_SECRET=
DXOS_APPROVER_CLIENT_ID=
DXOS_APPROVER_CLIENT_SECRET=
```

## 3. Chạy setup

Trên Windows, chạy tại thư mục gốc TTCSN:

```powershell
.\scripts\setup-dxos.ps1
```

Hoặc chạy trực tiếp bằng Bash:

```bash
bash scripts/setup-dxos.sh
```

Lệnh thực hiện tuần tự:

1. Kiểm tra Docker, Compose, `.env` và model YAML.
2. Tải artifact chính thức DX-OS `v2.0.0` và kiểm tra SHA-256 đã ghim.
3. Cài bộ runtime vào `.local/dxos`; không cần source code DX-OS.
4. Pull và chạy toàn bộ nền tảng DX-OS bằng Compose của release, gồm PostgreSQL, Keycloak, SeaweedFS, backend và APISIX.
5. Khởi tạo hoặc cập nhật model bằng CLI đóng gói trong release; thay đổi chỉ được chấp nhận tới mức `DXOS_ACCEPT_LEVEL` đã khai báo.
6. Cấp service client `REQUESTER` và `APPROVER` bằng API quản trị của DX-OS.
7. Ghi URL và credential sinh ra vào `.env`.
8. Build và chạy MySQL, backend, frontend TTCSN.
9. Chờ toàn bộ stack healthy.

Sau khi hoàn tất:

- TTCSN: `http://localhost:3000`.
- API: `http://localhost:8080`.
- DX-OS gateway: cổng được cấu hình bởi bản DX-OS, mặc định demo là `19080`.

Script có thể chạy lại.
Artifact tải về được cache trong `.local/cache/dxos` và runtime/data nằm trong `.local/dxos`; cả hai đều bị Git bỏ qua.
Nếu `.env` đã có đủ credential, script giữ nguyên service client hiện tại.
Nếu thay đổi model cần `MIGRATION` hoặc `LOSS` nhưng `.env` chỉ cho phép `SAFE`, setup dừng trước khi thay đổi dữ liệu.

## Dữ liệu Local đã tồn tại

Với database mới, không cần migration.
Nếu TTCSN đã có approval từ provider `local`, sau setup phải chạy preflight và migration qua API admin trước khi xử lý các đơn cũ:

```text
GET  /api/v1/admin/approval-migration/preflight
POST /api/v1/admin/approval-migration
```

Chỉ migrate khi preflight không có `issues`.
Không xóa volume MySQL để né migration.

## Giới hạn

Setup này loại bỏ cấu hình Keycloak thủ công và password grant.
TTCSN dùng hai service client theo chuẩn `client_credentials`.
DX-OS được ghim ở `v2.0.0`; nâng phiên bản phải cập nhật đồng thời version, URL và SHA-256 trong `scripts/dxos-release.env`.
Nó chưa bổ sung exactly-once cho action, expected-version nguyên tử, webhook reconciliation, TLS hoặc secret manager production.
