# Kết quả PoF Dynamic Core DX-OS

## Phạm vi đã chạy

PoF được chạy ngày 2026-11-10 bằng một Docker project và thư mục bind riêng của TTCSN.
Stack DX-OS hiện có của người dùng không được khởi động lại, sửa dữ liệu hoặc dùng làm nguồn bằng chứng.
Mọi thao tác model, record, action và event đều đi qua API công khai của DX-OS.

Mã DX-OS được kiểm chứng ở commit `5861c8bf5fb1e1c76c3b60a0ddb9e36d09f52c3c`.
PoF không đọc hoặc sửa mã Java của DX-OS.

## Kết quả theo phiên bản model

| Phiên bản | Thay đổi | Kết quả runtime |
| --- | --- | --- |
| V1 | Tạo model approval một bước | Plan `SAFE`, apply thành version 1; kiểm tra quyền, approve, reject có lý do và withdraw đạt |
| V2 | Đổi `routing_note` thành `review_note`, thêm `priority` bắt buộc có default | Plan `MIGRATION`, apply thành version 2; dữ liệu cũ giữ nguyên và nhận `priority=normal` |
| V3 | Thêm trạng thái `reviewing` và action `start_review` | Khi còn record mở, plan báo một rejection và apply trả 409; sau khi đóng record, apply thành version 3 |

Record tạo sau V3 không thể đi thẳng từ `pending` sang `accepted`.
Chuỗi `pending → reviewing → accepted` chạy đúng bằng action API.

Sau khi restart backend DX-OS, model vẫn ở version 3 với hash `742d08b170de7efd439fb486db24473183a67654e429845af96d88e933254c1a`.
Record kiểm tra vẫn ở `accepted`, version 2.
Events API replay trả 11 event và chứa đủ chuỗi `created`, `start_review`, `approve` của record kiểm tra.

## Điều PoF chứng minh

Một model YAML có thể khai báo schema, quyền và topology workflow để DX-OS tạo API runtime tương ứng.
Thay model YAML có thể đổi schema và topology mà không sửa hoặc build lại TTCSN.
DX-OS có guard không cho đổi flow khi còn hồ sơ mở trong phép thử này.
Version, model hash, record và event tồn tại qua restart.

Kết quả này chứng minh tính dynamic ở ranh giới model và runtime API.
Kết quả không chứng minh rằng một ứng dụng bất kỳ có thể tích hợp hoàn toàn chỉ bằng một file YAML.

## Hạn chế contract còn lại sau tích hợp MVP

Action API công khai của DX-OS nhận model, entity, record ID, tên action và một map dữ liệu tự do.
OpenAPI runtime không khai báo idempotency key hoặc expected version cho action command.
Gửi lại `approve` cho record đã ở trạng thái cuối trả `409 ERR_INVALID_STATE` thay vì trả lại kết quả của lần xử lý đầu.

Vì vậy `DxosApprovalAdapter` chỉ có thể dùng ledger cục bộ và preflight version theo hướng best-effort khi client timeout, retry hoặc hai actor gửi lệnh đồng thời.
Đây là khoảng trống hợp đồng của DX-OS, không còn là coupling trong domain TTCSN.

Meta API trả state, action, label và yêu cầu lý do ở cấp model.
DX-OS chưa có endpoint record-specific `availableActions`; adapter có thể suy ra từ state và metadata, nhưng một API trực tiếp sẽ giảm coupling và race giữa read với execute.

Đăng ký OIDC client, cấu hình Keycloak, ánh xạ subject, truyền token người dùng, lưu mapping record và tiếp nhận event vẫn là integration glue bình thường.
Các việc này không hợp lý khi nhét vào model YAML và không được coi là lỗi dynamic core.

## Phân định trách nhiệm

| Nhóm | Trạng thái |
| --- | --- |
| TTCSN | Đã có `ApprovalPort`, local adapter, semantic action, metadata động, audit, idempotency, optimistic locking và stack Docker độc lập |
| Tích hợp | Đã có `DxosApprovalAdapter`, mapping record và hai identity OIDC cố định cho MVP; chưa có per-user token forwarding, event checkpoint hoặc webhook |
| DX-OS | Cần contract action hỗ trợ retry idempotent và concurrency/version để thay local adapter mà không hạ chuẩn an toàn |

## Kết luận

TTCSN đã tích hợp adapter DX-OS ở mức MVP và không cần refactor domain thêm để chạy luồng demo.
DX-OS đạt bài kiểm tra dynamic schema và dynamic topology bằng model YAML.
DX-OS chưa đạt tuyên bố “cắm vào là dùng ngay chỉ với một file cấu hình” cho ứng dụng có yêu cầu exact retry và optimistic concurrency như TTCSN.
