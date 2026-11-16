# Cấu hình demo TTCSN từ Local sang DX-OS

## 1. Mục tiêu

Runbook này bắt đầu với TTCSN chạy độc lập bằng Local Workflow và database MySQL hiện có.
DX-OS được dựng thành một stack riêng, nhận model xét duyệt, sau đó TTCSN mới chuyển provider và migrate approval cũ.

Kết quả cuối cùng cần đạt:

- TTCSN vẫn dùng database MySQL ban đầu.
- Approval cũ được ánh xạ sang record DX-OS và giữ lịch sử Local.
- Approval mới được tạo trực tiếp trên DX-OS.
- Frontend lấy state và action từ metadata DX-OS.

Các lệnh TTCSN trong tài liệu dùng PowerShell tại thư mục gốc repository này.
Các lệnh `dxos` dùng Bash hoặc WSL tại repository hoặc thư mục cài đặt DX-OS.

## 2. Điều kiện chuẩn bị

- Docker Desktop đang hoạt động và `docker info` trả kết quả thành công.
- Các cổng mặc định `3000`, `8080`, `3306` và `19080` không bị chiếm.
- Đã có một bản DX-OS chứa CLI `deploy/dxos` và hỗ trợ model format của ba file trong `docs/dxos-pof/models/`.
- Không dùng stack DX-OS production hoặc dữ liệu của stack khác cho demo này.
- Không ghi JWT secret, mật khẩu DX-OS hoặc token vào Git.

Nếu cổng MySQL `3306` đã được dùng, đặt `MYSQL_HOST_PORT=3307` trong `.env`.
Việc đổi cổng host không thay đổi kết nối nội bộ từ backend tới service `mysql:3306`.

## 3. Giai đoạn A — Chạy TTCSN không có DX-OS

### 3.1. Tạo cấu hình TTCSN

Chỉ sao chép file mẫu khi `.env` chưa tồn tại:

```powershell
if (-not (Test-Path .env)) {
  Copy-Item .env.example .env
}
```

Sinh một JWT secret Base64 có 32 byte ngẫu nhiên:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Điền các giá trị sau vào `.env`:

```dotenv
SPRING_PROFILES_ACTIVE=demo
APPROVAL_PROVIDER=local

MYSQL_HOST_PORT=3306
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=3000

JWT_SECRET=<base64-secret-vua-sinh>
MFA_ENCRYPTION_KEY=<base64-secret-khac-hoac-cung-JWT_SECRET-cho-demo>
```

Không cần điền các biến `DXOS_*` ở giai đoạn này.

### 3.2. Khởi động TTCSN Local

```powershell
docker compose -p ttcsn-demo up -d --build --wait
docker compose -p ttcsn-demo ps
```

Kiểm tra:

- Website: `http://localhost:3000`.
- Swagger: `http://localhost:8080/swagger-ui.html`.
- Admin demo: `admin@mocnhien.vn` / `admin123`.
- Customer demo: `nguyen.van.a@gmail.com` / `123456`.

Ở trạng thái này, việc tắt hoặc chưa cài DX-OS không được làm TTCSN lỗi.

### 3.3. Chuẩn bị dữ liệu trước tích hợp

Tạo ít nhất bốn đơn để quan sát các trường hợp `PENDING`, `ACCEPTED`, `REJECTED` và `WITHDRAWN`.
Giữ ít nhất một đơn ở trạng thái chờ để tiếp tục xử lý sau khi migrate.
Ghi lại mã đơn, approval status và fulfillment status trước khi chuyển đổi.

Không xóa volume `ttcsn-demo_mysql_data` trong suốt kịch bản.

## 4. Giai đoạn B — Khởi động và cấu hình DX-OS

### 4.1. Dùng một stack DX-OS riêng

Phần dưới giả định biến `DXOS_REPO` trỏ tới repository DX-OS và `deploy/.env` đã được tạo theo hướng dẫn cài đặt của DX-OS.

Ví dụ trong Bash hoặc WSL:

```bash
export DXOS_REPO=/path/to/DX-Farm
cd "$DXOS_REPO"
```

Trong `deploy/.env`, dùng cổng gateway riêng cho demo:

```dotenv
COMPOSE_PROJECT_NAME=ttcsn-dxos-demo
DXOS_MODE=core
APISIX_HOST_PORT=19080
PUBLIC_HOST=localhost
GATEWAY_PUBLIC_URL=http://${PUBLIC_HOST}:${APISIX_HOST_PORT}
KEYCLOAK_ADMIN_CONSOLE=true
```

Giữ riêng thư mục dữ liệu PostgreSQL và object storage của stack demo.
Không trỏ cấu hình này vào thư mục dữ liệu của một stack DX-OS đang sử dụng.

Khởi động và kiểm tra DX-OS:

```bash
./deploy/dxos start --core
./deploy/dxos status
```

Gateway phải phản hồi tại `http://localhost:19080`.
Nếu cần tạo lại tài khoản quản trị tạm thời, chạy `./deploy/dxos admin` và đổi mật khẩu ngay lần đăng nhập đầu tiên.

### 4.2. Áp dụng model V1

```bash
./deploy/dxos lint \
  /path/to/TTCSN/docs/dxos-pof/models/v1.yaml

./deploy/dxos plan \
  /path/to/TTCSN/docs/dxos-pof/models/v1.yaml

./deploy/dxos apply \
  /path/to/TTCSN/docs/dxos-pof/models/v1.yaml \
  --accept SAFE
```

Sau khi apply, model `ttcsn_order_approval` phải có entity `approval` và workflow `pending → accepted/rejected`.

### 4.3. Nâng model V1 → V2 → V3

Plan từng phiên bản trước khi apply và chỉ chấp nhận đúng mức rủi ro mà plan trả về.
Không dùng `--accept LOSS` cho kịch bản này.

```bash
./deploy/dxos plan \
  /path/to/TTCSN/docs/dxos-pof/models/v2-schema.yaml

./deploy/dxos apply \
  /path/to/TTCSN/docs/dxos-pof/models/v2-schema.yaml \
  --accept MIGRATION
```

Đóng mọi record thử nghiệm còn mở trước khi đổi topology sang V3.
DX-OS phải từ chối đổi topology nếu vẫn còn record mở.

```bash
./deploy/dxos plan \
  /path/to/TTCSN/docs/dxos-pof/models/v3-topology.yaml

./deploy/dxos apply \
  /path/to/TTCSN/docs/dxos-pof/models/v3-topology.yaml \
  --accept MIGRATION
```

Model cuối cùng phải có chuỗi `pending → reviewing → accepted/rejected` và action `start_review`.

## 5. Giai đoạn C — Cấu hình OIDC cho TTCSN

MVP hiện dùng một identity requester và một identity approver dùng chung.
Không sử dụng tài khoản quản trị DX-OS làm credential nghiệp vụ.

Mở Keycloak Admin Console tại `http://localhost:19080/admin/`, chọn realm `dxos` và cấu hình:

1. Tạo public client có Client ID `ttcsn-mvp`.
2. Tắt `Client authentication`.
3. Tắt `Standard flow`.
4. Bật `Direct access grants`.
5. Thêm audience mapper `dxos-api` vào access token.
6. Tạo user `ttcsn-requester` với mật khẩu không tạm thời.
7. Tạo user `ttcsn-approver` với mật khẩu không tạm thời.
8. Gán client role `REQUESTER` của client `dxos-m-ttcsn_order_approval` cho `ttcsn-requester`.
9. Gán client role `APPROVER` của client `dxos-m-ttcsn_order_approval` cho `ttcsn-approver`.

Token requester và approver phải có audience `dxos-api`.
Role model có thể nằm trong `resource_access.dxos-m-ttcsn_order_approval.roles`, không nhất thiết nằm trong `realm_access`.

## 6. Giai đoạn D — Chuẩn bị TTCSN để cutover

Khi TTCSN vẫn đang chạy bằng provider `local`, điền trước cấu hình kết nối vào `.env`:

```dotenv
DXOS_BASE_URL=http://host.docker.internal:19080
DXOS_MODEL=ttcsn_order_approval
DXOS_ENTITY=approval
DXOS_REALM=dxos
DXOS_CLIENT_ID=ttcsn-mvp
DXOS_CLIENT_SECRET=
DXOS_CONNECT_TIMEOUT=3s
DXOS_READ_TIMEOUT=8s

DXOS_REQUESTER_USERNAME=ttcsn-requester
DXOS_REQUESTER_PASSWORD=<requester-password>
DXOS_APPROVER_USERNAME=ttcsn-approver
DXOS_APPROVER_PASSWORD=<approver-password>
```

Giá trị `host.docker.internal` được dùng vì backend TTCSN chạy trong container còn gateway DX-OS được publish trên máy Windows.
Nếu backend TTCSN chạy trực tiếp trên host, dùng `DXOS_BASE_URL=http://localhost:19080`.

## 7. Giai đoạn E — Cutover và migrate approval cũ

### 7.1. Chuyển provider

Đổi đúng một dòng trong `.env`:

```dotenv
APPROVAL_PROVIDER=dxos
```

Chỉ tạo lại backend, không tạo lại MySQL và không xóa volume:

```powershell
docker compose -p ttcsn-demo up -d --build --no-deps --force-recreate backend
docker compose -p ttcsn-demo ps
```

Sau khi backend healthy, chạy migration trước khi mở lại danh sách đơn hàng.

### 7.2. Lấy token Admin TTCSN

```powershell
$loginBody = @{
  email = 'admin@mocnhien.vn'
  password = 'admin123'
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/auth/login' `
  -ContentType 'application/json' `
  -Body $loginBody

$headers = @{
  Authorization = "Bearer $($login.data.accessToken)"
}
```

### 7.3. Chạy preflight

```powershell
$preflight = Invoke-RestMethod `
  -Method Get `
  -Uri 'http://localhost:8080/api/v1/admin/approval-migration/preflight' `
  -Headers $headers

$preflight.data | ConvertTo-Json -Depth 8
```

Chỉ tiếp tục khi `issues` rỗng và `eligibleByStatus` khớp số approval Local đã chuẩn bị.
Preflight không tạo record DX-OS và không sửa dữ liệu MySQL.

### 7.4. Chạy migration

```powershell
$migration = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/admin/approval-migration' `
  -Headers $headers

$migration.data | ConvertTo-Json -Depth 8
```

Kết quả đạt khi:

- `failed` bằng `0`.
- `migrated` bằng số approval đủ điều kiện trước đó.
- Mỗi item có `outcome=MIGRATED` và có `dxosApprovalRef`.
- Gọi lại preflight trả `eligible=0`.
- Không có hai record DX-OS cùng `order_id`.

Migration giữ lịch sử Local, thêm mốc `MIGRATE_TO_DXOS` và replay trạng thái đích theo metadata workflow hiện tại.
Nếu một record đã lưu liên kết DX-OS nhưng chưa replay xong, lượt migration tiếp theo tiếp tục từ record remote đó thay vì tạo record mới.

## 8. Giai đoạn F — Kiểm tra sau tích hợp

### 8.1. Đơn cũ

1. Mở một đơn đã tồn tại trước cutover.
2. Kiểm tra lịch sử có sự kiện Local, mốc `MIGRATE_TO_DXOS` và sự kiện DX-OS.
3. Với đơn đang chờ, thực hiện `Tiếp nhận` rồi `Duyệt` hoặc `Từ chối`.
4. Kiểm tra trạng thái TTCSN và record DX-OS khớp nhau.

### 8.2. Đơn mới

1. Đăng nhập Customer TTCSN.
2. Tạo một đơn mới.
3. Xác nhận MySQL có order mới và DX-OS có approval record tương ứng.
4. Đăng nhập Admin TTCSN.
5. Thực hiện `Tiếp nhận → Duyệt`.
6. Tiếp tục fulfillment trên TTCSN.

### 8.3. Khởi động lại

```powershell
docker compose -p ttcsn-demo restart backend
```

Sau restart, danh sách đơn, mapping approval, lịch sử và action hiện có phải đọc lại bình thường.

## 9. Kiểm tra nhanh khi có lỗi

### TTCSN trả `APPROVAL_MIGRATION_REQUIRED`

Approval vẫn là nguồn `LOCAL` hoặc lượt migration trước chưa hoàn tất.
Chạy lại preflight rồi migration trước khi sử dụng trang đơn hàng.

### TTCSN trả `APPROVAL_PROVIDER_UNAVAILABLE`

Kiểm tra `DXOS_BASE_URL`, trạng thái gateway, credential OIDC và audience `dxos-api`.

```powershell
Invoke-WebRequest 'http://localhost:19080/realms/dxos/.well-known/openid-configuration'
```

Không dùng đường dẫn Keycloak cũ có tiền tố `/auth/realms/`.

### DX-OS từ chối action

Kiểm tra role nằm đúng client `dxos-m-ttcsn_order_approval` và tải lại order để lấy workflow state/version mới nhất.

### V3 bị từ chối khi apply

Hoàn tất hoặc rút mọi record đang mở của V1/V2, chạy lại `dxos plan`, rồi mới apply V3.
Không bỏ qua guard bằng cách xóa trực tiếp dữ liệu DX-OS.

## 10. Giới hạn của bản demo

- MVP dùng hai identity DX-OS dùng chung, chưa chuyển token theo từng người dùng TTCSN.
- Action API DX-OS chưa nhận idempotency key và expected version trong cùng command.
- Có một cửa sổ lỗi nhỏ nếu DX-OS đã tạo record nhưng tiến trình dừng trước khi TTCSN lưu mapping.
- Demo chưa thay thế secret manager, TLS, webhook reconciliation, monitoring hoặc quy trình production.

Không tuyên bố production-ready chỉ dựa trên việc hoàn thành runbook này.
