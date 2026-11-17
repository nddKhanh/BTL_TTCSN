# Ranh giới workflow đơn hàng

## Phạm vi

BTL_TTCSN vẫn là ứng dụng bán cà phê độc lập.
Danh mục, giỏ hàng, định giá, coupon, COD, giao hàng và dashboard thuộc ứng dụng.
Phần xét duyệt đi qua `ApprovalPort` và hiện được thực thi bởi `LocalApprovalAdapter`.
`APPROVAL_PROVIDER` mặc định là `local`; adapter tương lai đăng ký implementation tương ứng với provider mới.
Không có tên trạng thái, endpoint, token hoặc thư viện DX-OS trong domain ứng dụng.

## Trạng thái và action

Xét duyệt chuẩn hóa kết quả về `PENDING`, `ACCEPTED`, `REJECTED` và `WITHDRAWN`.
Workflow state và action name là chuỗi động do implementation của `ApprovalPort` cung cấp.
Local adapter hiện dùng `pending`, `accepted`, `rejected`, `withdrawn` cùng các action `approve`, `reject` và `withdraw`.
Frontend hiển thị label và yêu cầu lý do từ action metadata thay vì hardcode toàn bộ action của adapter.
Thực hiện đơn dùng `NOT_STARTED`, `PREPARING`, `DELIVERING`, `COMPLETED` và `CANCELLED`.
Action hợp lệ là `startPreparation`, `dispatch`, `complete` và `cancel`.

`OrderStatus` cũ chỉ còn là projection tương thích:

| Approval | Fulfillment | `OrderStatus` tương thích |
| --- | --- | --- |
| `PENDING` | `NOT_STARTED` | `PENDING` |
| `ACCEPTED` | `NOT_STARTED` hoặc `PREPARING` | `CONFIRMED` |
| `ACCEPTED` | `DELIVERING` | `DELIVERING` |
| `ACCEPTED` | `COMPLETED` | `COMPLETED` |
| `REJECTED` hoặc `WITHDRAWN` | `CANCELLED` | `CANCELLED` |
| `ACCEPTED` | `CANCELLED` | `CANCELLED` |

Không service hoặc controller nào được ghi quyết định xét duyệt trực tiếp vào `OrderStatus`.

## Quyền và danh tính

Actor đã đăng nhập dùng định danh nội bộ `user:<id>`.
Định danh này ổn định khi email thay đổi và có thể liên kết với subject SSO trong phase tích hợp.
Đơn khách vãng lai dùng actor local `guest-order:<orderCode>` và luôn chạy qua local adapter.
Actor khách vãng lai không được chuyển thành danh tính DX-OS giả.

Chủ đơn đã đăng nhập có thể rút yêu cầu còn `PENDING`.
Người có `ORDER_APPROVE` có thể chấp nhận hoặc từ chối yêu cầu còn `PENDING`.
Người tạo đơn không được tự duyệt dù đồng thời có `ORDER_APPROVE`.
Người có `ORDER_FULFILL` mới được thực hiện action giao hàng.
Backend kiểm tra lại quyền, trạng thái và version cho mọi command.

## Tính nhất quán

Tạo order và approval local nằm trong một transaction.
`@Version` bảo vệ order và approval khỏi lost update.
Mỗi lịch sử action có unique key theo workflow và `requestId`.
Replay cùng request ID và action không tạo thêm quyết định hoặc hiệu ứng.
Replay khác action, sai version hoặc transition sai trả `409 WORKFLOW_CONFLICT`.
Trạng thái cuối của approval và fulfillment là bất biến.

## Ranh giới adapter tương lai

Adapter tương lai phải triển khai cùng `ApprovalPort` và giữ nguyên ý nghĩa nghiệp vụ của status, action, actor, version, history và request ID.
Adapter chỉ nhận đơn có actor xác thực đủ điều kiện.
BTL_TTCSN chỉ áp dụng kết quả cuối chuẩn hóa và vẫn sở hữu fulfillment.
Mapping `orderId` với record bên ngoài, webhook, checkpoint và dedup nguồn ngoài thuộc phase tích hợp DX-OS sau này.
DX-OS action API được thử hiện chưa nhận request ID hoặc expected version.
Không cutover sang adapter ngoài cho đến khi contract đó được bổ sung hoặc có một cơ chế tương đương được chứng minh qua retry và concurrent command.

## Backfill dữ liệu cũ

Migration ánh xạ `PENDING` sang approval `PENDING` và fulfillment `NOT_STARTED`.
Migration ánh xạ `CONFIRMED` sang approval `ACCEPTED` và fulfillment `PREPARING`.
Migration ánh xạ `DELIVERING` hoặc `COMPLETED` sang approval `ACCEPTED` và fulfillment cùng nghĩa.
Migration ánh xạ `CANCELLED` sang approval `WITHDRAWN` và fulfillment `CANCELLED` vì dữ liệu cũ không lưu nguyên nhân hủy.
Mỗi record backfill có history action `MIGRATE` để không bị nhầm với quyết định của người dùng.
