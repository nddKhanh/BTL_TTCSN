# Kế hoạch tái cấu trúc BTL_TTCSN trước khi tích hợp DX-OS

## 1. Mục tiêu và phạm vi

**Mục tiêu:** Hoàn thiện [BTL_TTCSN](https://github.com/nddKhanh/BTL_TTCSN) thành ứng dụng bán cà phê **độc lập**, có quy trình rõ ràng, mã dễ bảo trì và một ranh giới tích hợp ổn định để về sau chuyển **quy trình xét duyệt đơn** sang DX-OS. Đây là giai đoạn **chuẩn bị ứng dụng, chưa cắm DX-OS**.

**Nguyên tắc bắt buộc:**

- Ứng dụng vẫn chạy đầy đủ khi không cài hoặc kết nối DX-OS.
- Giữ logic danh mục, giỏ hàng, giá/coupon, đặt hàng COD, quản lý giao hàng và dashboard ở BTL_TTCSN.
- Phần xét duyệt đơn có **một nơi ra quyết định**. Ban đầu là local workflow; sau này có thể thay thế bằng adapter tới DX-OS.
- Không đưa tên trạng thái, endpoint, token hoặc thư viện riêng của DX-OS vào domain của BTL_TTCSN.
- Không tự xây một workflow engine tổng quát; chỉ xử lý quy trình của ứng dụng đủ đúng và đủ kiểm thử.
- Mỗi thay đổi giữ nguyên hành vi ngoài phạm vi refactor, có migration và test trước/sau.

## 2. Hiện trạng đã đối chiếu ở branch `main`

| Nguồn | Hiện trạng | Việc cần làm |
|---|---|---|
| `backend/.../service/OrderService.java` | Gộp tạo đơn, tính tiền, truy vấn, thống kê, chuyển trạng thái và hủy đơn (~312 dòng) | Tách theo use case và trách nhiệm |
| `backend/.../model/OrderStatus.java` | `PENDING → CONFIRMED → DELIVERING → COMPLETED`, có nhánh `CANCELLED`; `canTransitionTo()` hardcode | Tách xét duyệt với thực hiện/giao hàng và chính sách hành động |
| `backend/.../controller/OrderController.java` | Có endpoint admin nhận `requestedStatus` | Thay bằng command/hành động nghiệp vụ; không cho đổi status tùy ý |
| `backend/.../model/Order.java` | Trạng thái tổng quát, ngày tạo; chưa thấy version/lịch sử quyết định tích hợp | Bổ sung version, thông tin liên kết quy trình và lịch sử |
| `backend/.../config/SecurityConfig.java` | Spring Security + JWT nội bộ; có luồng đặt hàng không cần đăng nhập | Giữ chế độ độc lập, giới hạn luồng xét duyệt tích hợp tương lai ở đơn có chủ thể xác thực |
| Frontend | Các trang đặt hàng, theo dõi và quản trị | Hiển thị hành động do backend trả về, không quyết định quyền trên client |

Các nhận định trên dựa vào mã đã đọc, **không** đồng nghĩa đã chạy build/E2E.

## 3. Quy trình nghiệp vụ đích

Tách hai khái niệm để DX-OS về sau chỉ nhận phần phù hợp với miền *hồ sơ có người tham gia*.

### 3.1. Quy trình A: Xét duyệt yêu cầu đặt hàng (có thể chuyển sang DX-OS)

1. Khách đăng nhập tạo đơn; backend xác minh sản phẩm, giá/coupon và lưu snapshot đơn.
2. Tạo **yêu cầu xét duyệt** gắn với `orderId`; trạng thái ban đầu `PENDING`.
3. Nhân viên có quyền xem và quyết định **chấp nhận** hoặc **từ chối** với lý do khi cần.
4. Chủ đơn có thể **rút/hủy yêu cầu** khi chính sách cho phép; không ai được rút hộ.
5. Chấp nhận → kích hoạt thực hiện đơn; từ chối/rút → kết thúc yêu cầu nhưng không coi là thành công.
6. Lưu actor, thời điểm, hành động, kết quả, lý do và phiên bản; trạng thái cuối bất biến.

**Điều kiện an toàn:** chỉ người có quyền mới quyết định; người tạo không được tự phê duyệt dù đồng thời mang vai nhân viên; lệnh lặp hoặc đồng thời không tạo hai quyết định. Cần thống nhất riêng cách xử lý đơn **khách vãng lai**: giữ luồng cũ ngoài phạm vi tích hợp hoặc yêu cầu đăng nhập cho tập đơn thuộc quy trình A; không giả mạo danh tính để gửi DX-OS.

### 3.2. Quy trình B: Thực hiện đơn hàng (ở BTL_TTCSN)

`ACCEPTED → PREPARING → DELIVERING → COMPLETED`, với các nhánh thất bại/hủy hợp lệ được đặc tả. Chỉ đơn **được chấp nhận** mới được bắt đầu chuẩn bị. Việc định giá, vận chuyển, doanh thu và các hiệu ứng thương mại là trách nhiệm ứng dụng. Không dùng trạng thái xét duyệt của lõi để đại diện toàn bộ trạng thái giao hàng.

### 3.3. Quy ước trạng thái

- `ApprovalStatus`: `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN` (trạng thái nghiệp vụ ổn định của ứng dụng; không đồng nhất tên trạng thái bên trong DX-OS).
- `FulfillmentStatus`: `NOT_STARTED`, `PREPARING`, `DELIVERING`, `COMPLETED`, có thể bổ sung nhánh `CANCELLED` theo chính sách.
- `OrderStatus` hiện có được giữ tương thích API trong giai đoạn chuyển đổi; thiết kế bảng ánh xạ rõ và chỉ ngừng dùng khi frontend/test đã chuyển đổi.
- Tất cả API cập nhật dùng **hành động** (`approve`, `reject`, `withdraw`, `startPreparation`, `dispatch`, `complete`), không nhận trạng thái đích tùy ý.

## 4. Kiến trúc ứng dụng mục tiêu

```text
com.highlands.order/
├── api/
│   ├── order/                 # REST, DTO, validation
│   ├── approval/
│   └── fulfillment/
├── application/
│   ├── order/
│   │   ├── PlaceOrderService
│   │   ├── CancelOrderService
│   │   └── OrderQueryService
│   ├── approval/
│   │   ├── ApprovalPort
│   │   └── ApprovalApplicationService
│   └── fulfillment/
│       └── FulfillmentService
├── domain/
│   ├── order/
│   ├── approval/
│   ├── pricing/
│   └── fulfillment/
└── infrastructure/
    ├── persistence/
    ├── security/
    └── approval/
        └── LocalApprovalAdapter
```

**Không cần di chuyển mọi package cùng lúc.** Tách theo luồng nghiệp vụ và giữ build xanh sau mỗi PR.

### 4.1. Hợp đồng `ApprovalPort`

Hợp đồng chỉ diễn tả nghiệp vụ ứng dụng:

- `submit(orderId, actor, payload, requestId)` → `approvalRef`.
- `get(approvalRef, actor)` → trạng thái, phiên bản, thông tin hiển thị.
- `availableActions(approvalRef, actor)` → hành động hợp lệ ở thời điểm đọc (backend vẫn xác thực lại khi thực hiện).
- `execute(approvalRef, action, actor, reason, requestId, expectedVersion)` → kết quả mới.
- `history(approvalRef, actor)` → lịch sử được phép đọc.

Tên phương thức là đề xuất, có thể điều chỉnh theo Java interface và khả năng của adapter. Giữ `approvalRef` độc lập với ID của DX-OS; `actor` là định danh nội bộ ổn định để có thể liên kết với `sub` của hệ SSO tương lai.

### 4.2. Quyền quyết định và hiệu ứng ứng dụng

- `LocalApprovalAdapter` sở hữu trạng thái xét duyệt khi chạy độc lập.
- Service đặt đơn chịu trách nhiệm tạo đơn và đăng ký yêu cầu một cách nhất quán; xử lý atomicity nếu dùng transaction chung.
- Application service xử lý **kết quả cuối** thành hành động BTL (mở chuẩn bị hoặc ghi từ chối/hủy), đảm bảo idempotent và không phát hiệu ứng hai lần.
- Không có service/controller khác trực tiếp ghi trạng thái xét duyệt để lách quy trình.
- Khi về sau có `DxosApprovalAdapter`, quy trình A do DX-OS quyết định; BTL chỉ nhận và áp dụng kết quả hợp lệ. Việc dùng DX-OS vẫn cần **mã tích hợp mới ở giai đoạn sau**, không phải chỉ đổi một biến cấu hình.

## 5. Kế hoạch thực hiện theo PR

| PR | Công việc | Điều kiện hoàn thành |
|---|---|---|
| **P0 — Baseline** | Chốt requirements và ma trận chuyển trạng thái; chạy build/test, lập E2E độc lập; tạo tag/commit mốc | Chứng minh đặt → duyệt → giao → hoàn thành và nhánh hủy mà không có DX-OS |
| **P1 — Domain** | Tạo `ApprovalStatus`, `FulfillmentStatus`, policy hành động và các bất biến; tránh sửa thẳng status | Unit test đủ luồng được phép và bị cấm |
| **P2 — Service cleanup** | Tách `OrderService` theo use case, đưa tính giá về service thuần, tách dashboard query; controller mỏng | Test cũ xanh, API hiện tại không bị phá ngoài thay đổi đã công bố |
| **P3 — Persistence** | Flyway migration tạo dữ liệu xét duyệt, event/history, `version`, khóa ngoại và unique constraint; backfill đơn cũ | Không mất dữ liệu, rollback/restore có hướng dẫn, concurrency test xanh |
| **P4 — Local adapter** | `ApprovalPort` + `LocalApprovalAdapter`; application service nhận kết quả theo hợp đồng; khóa đường cập nhật status trực tiếp | Tất cả quyết định đi qua port, ứng dụng vẫn chạy standalone |
| **P5 — API & UI** | Hành động có ngữ nghĩa, DTO gồm trạng thái và `availableActions`; admin/khách xem lịch sử, lý do, quyền | Không hardcode bước xét duyệt trên frontend; 403/409 chính xác |
| **P6 — Hardening** | Idempotency theo request ID, optimistic locking, audit, authz, phục hồi lỗi, regression test, docs | Build/test/E2E xanh; đủ checklist bàn giao cho phase tích hợp |

Đừng thêm phê duyệt nhiều tầng vào BTL lúc này chỉ để giống DX-OS. Chạy local theo một bước duyệt rõ ràng; **bước duyệt bổ sung** sẽ là kịch bản thay đổi mô hình sau khi tích hợp lõi.

## 6. Chi tiết dữ liệu và giao diện

### 6.1. Mô hình dữ liệu tối thiểu (đề xuất)

- `orders`: các trường thương mại hiện có, `fulfillment_status`, `version` và liên kết tới hồ sơ duyệt (nếu cần).
- `order_approvals`: `id`, `order_id` (unique cho mỗi lần duyệt theo chính sách), `status`, `created_by`, `created_at`, `decided_by`, `decided_at`, `version`.
- `approval_history`: `id`, `approval_id`, `action`, `actor_id`, `from_status`, `to_status`, `reason`, `created_at`, `request_id`; unique `request_id` trong phạm vi phù hợp.
- Cân nhắc bảng `processed_effects`/idempotency để chắc chắn việc chấp nhận không kích hoạt thực hiện đơn hai lần, kể cả sau khi khởi động lại.

Không ghi trực tiếp dữ liệu domain BTL vào DB lõi. Chưa cần cột `dxos_record_id` ở phase refactor; có thể tạo bảng mapping riêng khi tích hợp.

### 6.2. API mục tiêu (minh họa)

```http
POST /api/v1/orders
GET  /api/v1/orders/{id}
GET  /api/v1/orders/{id}/approval
GET  /api/v1/orders/{id}/approval/actions
POST /api/v1/orders/{id}/approval/actions/{action}
GET  /api/v1/orders/{id}/approval/history
POST /api/v1/orders/{id}/fulfillment/actions/{action}
```

Phải giữ hoặc deprecate có lộ trình endpoint `/api/v1/orders/admin/{id}/status`, tránh client cũ lách luật. Không để frontend gọi API ngoài luồng mà service vẫn cho phép.

### 6.3. Frontend

- Khách hàng xem trạng thái xét duyệt **riêng** với trạng thái giao hàng; rút yêu cầu nếu được phép.
- Nhân viên thấy công việc đang chờ, chi tiết đơn, nút hành động từ `availableActions`, có ô lý do khi từ chối.
- Ẩn nút trên UI chỉ để hỗ trợ UX; **backend luôn tái kiểm tra quyền và trạng thái**.
- Hiển thị lịch sử với ai, lúc nào, hành động, kết quả.
- Giữ nguyên các trang mua hàng, thanh toán, danh mục, coupon và dashboard nếu không thuộc thay đổi.

## 7. Kiểm thử bắt buộc

1. **Độc lập:** backend + frontend + MySQL chạy khi DX-OS tắt/không tồn tại.
2. **Luồng đúng:** tạo → duyệt → chuẩn bị → giao → hoàn tất; từ chối và rút/hủy phản ánh kết quả khác thành công.
3. **Quyền:** khách không duyệt; người khác không rút hộ; người tạo kiêm nhân viên không tự duyệt; nhân viên có quyền duyệt được.
4. **Không lách:** gửi status trực tiếp hay gọi endpoint cũ không bỏ qua bước duyệt.
5. **Cạnh tranh:** hai người duyệt cùng lúc chỉ một quyết định hiệu lực, lệnh sai version trả 409.
6. **Idempotency:** gửi cùng request nhiều lần, kể cả sau restart, không tạo nhiều kết quả/hiệu ứng.
7. **Dữ liệu:** migration bảo toàn đơn cũ, phép tính tiền và báo cáo không thay đổi ngoài phần đã thiết kế.
8. **Regression:** giỏ hàng, catalog, coupon, đăng nhập, thống kê, quản trị sản phẩm và các chức năng khác vẫn chạy.
9. **Contract test:** cùng bộ test chạy qua `ApprovalPort` với local implementation; tài liệu xác định cách một adapter tương lai phải tuân thủ.

## 8. Điều kiện sẵn sàng cắm DX-OS

- [ ] Có tag/commit mốc chứng minh BTL_TTCSN hoạt động độc lập.
- [x] Quy trình xét duyệt A có hành động/trạng thái/quyền/kết quả cuối rõ ràng.
- [x] Quy trình giao hàng B không phụ thuộc bên xét duyệt nội bộ ngoài kết quả chuẩn hóa.
- [x] Không còn đường cập nhật quyết định xét duyệt bên ngoài `ApprovalPort`.
- [x] Local adapter chạy được toàn bộ luồng, không cần DX-OS.
- [x] Frontend không hardcode thứ tự bước và nút xét duyệt; backend cung cấp metadata hành động.
- [x] Có định danh người dùng ổn định, test quyền và chính sách chủ hồ sơ.
- [x] Có audit, chống trùng và kiểm soát đồng thời.
- [x] Có migration và rollback/restore, E2E cùng regression test xanh.
- [x] Có tài liệu ranh giới, hợp đồng kết quả cuối, danh tính và cách vận hành.

Tag/commit mốc chưa được tạo vì thay đổi Git chỉ được thực hiện khi người dùng yêu cầu riêng.
Kết quả PoF DX-OS và giới hạn contract action được ghi tại [`docs/dxos-pof/RESULTS.md`](dxos-pof/RESULTS.md).

## 9. Việc để dành cho phase tích hợp DX-OS (không làm bây giờ)

- Thiết kế model DX-OS thật theo họ quy trình được hỗ trợ, xác minh V1 → V2 bằng API `plan/apply`.
- Tích hợp Keycloak và liên kết `actorId` với `sub`; mọi thao tác trên lõi thực hiện bằng token chính người dùng, không tạo hộ bằng tài khoản quản trị.
- Viết `DxosApprovalAdapter`, bảng liên kết `orderId ↔ model/entity/recordId` và trình tiếp nhận webhook có chữ ký.
- Lưu sự kiện bền vững trước khi ACK, deduplicate theo nguồn + `eventId`, đọc bù `GET /api/v1/events` từ checkpoint an toàn; sử dụng tài khoản ADMIN riêng chỉ cho luồng phục hồi phía server.
- Phân biệt hoàn tất, từ chối, rút yêu cầu; không coi mọi trạng thái cuối là thành công.
- Kiểm chứng dynamic bằng đổi mô hình sau khi đã xử lý hết hồ sơ mở, giữ nguyên mã/bản chạy BTL, adapter và DX-OS.

## 10. Nguồn đối chiếu

- BTL: [`OrderService.java`](https://github.com/nddKhanh/BTL_TTCSN/blob/main/backend/src/main/java/com/highlands/order/service/OrderService.java), [`OrderStatus.java`](https://github.com/nddKhanh/BTL_TTCSN/blob/main/backend/src/main/java/com/highlands/order/model/OrderStatus.java), [`OrderController.java`](https://github.com/nddKhanh/BTL_TTCSN/blob/main/backend/src/main/java/com/highlands/order/controller/OrderController.java), [`README.md`](https://github.com/nddKhanh/BTL_TTCSN/blob/main/README.md).
- DX-OS: [`docs/dynamic-core/README.md`](https://github.com/HaUI-ImplSolution/DX-OS/blob/Khuong/main/docs/dynamic-core/README.md), [`docs/API.md`](https://github.com/HaUI-ImplSolution/DX-OS/blob/Khuong/main/docs/API.md), [`deploy/INTEGRATIONS.md`](https://github.com/HaUI-ImplSolution/DX-OS/blob/Khuong/main/deploy/INTEGRATIONS.md).
- Tài liệu định hướng người dùng cung cấp: `ung_dung.md` (mục tiêu độc lập → tích hợp → dynamic; tiêu chí danh tính, webhook, bảo vệ đổi mô hình).

> **Kết luận:** Sau P0–P6, BTL_TTCSN phải trở thành một ứng dụng bán cà phê độc lập, có ranh giới *approval* có thể thay thế. Giai đoạn sau mới tích hợp DX-OS để kiểm chứng lõi điều khiển quy trình thật và đổi mô hình an toàn, không sửa mã ứng dụng.
