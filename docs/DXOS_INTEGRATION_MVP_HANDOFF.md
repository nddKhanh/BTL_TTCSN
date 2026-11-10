# Handoff tích hợp DX-OS MVP

## Kết luận phạm vi source code

Phần tích hợp source code cho luồng approval MVP đã hoàn thành.

Khi `APPROVAL_PROVIDER=dxos`, TTCSN có thể tạo approval record qua API công khai của DX-OS, lưu mapping với order, đọc metadata action động, thực hiện action và đồng bộ trạng thái terminal về order.

Khi `APPROVAL_PROVIDER=local`, luồng approval cục bộ vẫn là fallback và không phụ thuộc DX-OS.

Kết luận này chỉ áp dụng cho MVP đã kiểm chứng bằng test tự động và API E2E.
Nó không bao gồm tự động hóa hạ tầng Keycloak, kiểm tra giao diện trực quan hoặc các bảo đảm production được liệt kê bên dưới.

## Phần đã triển khai

- `DxosApiClient` lấy token OIDC, cache token có thời hạn, đặt timeout và chuẩn hóa lỗi từ DX-OS.
- `DxosApprovalAdapter` triển khai `ApprovalPort` mà không đưa tên state hoặc action của model V3 vào controller và frontend.
- Mapping giữa order TTCSN và record DX-OS được lưu trong `order_approvals.approval_ref`.
- State DX-OS được chuẩn hóa về `ApprovalStatus` của TTCSN.
- Metadata action giữ nguyên `label` và `needsNote` để frontend sinh action động.
- Handler có quyền duyệt dùng identity approver; chủ đơn dùng identity requester và vẫn chịu kiểm tra owner cục bộ.
- `approval_history.request_id` được dùng làm ledger chống replay phía TTCSN.
- `expectedVersion` được kiểm tra trước khi gửi action.
- Migration V10 mở rộng `approval_ref` lên 100 ký tự và `action` lên 64 ký tự.
- Seeder không tạo order và approval local khi provider là `dxos`.
- `.env.example`, `application.yml` và `docker-compose.yml` đã có cấu hình DX-OS bằng placeholder.
- Không có mật khẩu hoặc token runtime của phiên kiểm tra được ghi vào source.

## Bằng chứng đã chạy

Backend Maven test suite đã chạy thành công.

Frontend Node test suite đã chạy thành công với 17 test.

Backend compile và Docker image build đã chạy thành công.

Một database TTCSN sạch và một stack DX-OS cô lập đã được dùng để chạy API E2E.

Luồng E2E đã xác nhận:

1. TTCSN tạo order mới và DX-OS tạo record tương ứng ở state `pending`, version `0`.
2. Metadata admin trả action `start_review` với label `Tiếp nhận`.
3. `start_review` chuyển record sang `reviewing`, version `1`; order TTCSN vẫn ở `PENDING`.
4. `approve` chuyển record sang `accepted`, version `2`; order TTCSN chuyển sang `CONFIRMED`.
5. Events API trả chuỗi `created`, `start_review`, `approve`.
6. Sau khi restart backend TTCSN, mapping và trạng thái vẫn đọc được.
7. Gửi lại cùng `requestId` không tạo thêm event và trả lại snapshot hiện có.

PoF về thay đổi model V1, V2 và V3 được ghi riêng tại [Kết quả PoF](dxos-pof/RESULTS.md).

## Các vấn đề chưa làm và cần tự kiểm tra

| Vấn đề | Trạng thái hiện tại | Việc cần kiểm tra hoặc bổ sung |
| --- | --- | --- |
| Kiểm tra giao diện trực quan | Chưa hoàn thành; mới có API E2E và test JavaScript | Đăng nhập customer và admin trên trình duyệt; xác nhận nút `Tiếp nhận`, `Duyệt`, `Từ chối`, yêu cầu lý do và trạng thái sau refresh |
| Bootstrap OIDC | Đã cấu hình thủ công trên stack thử nghiệm, chưa được tự động hóa trong source TTCSN | Tạo public client `ttcsn-mvp`, bật direct access grant, tắt standard flow và thêm audience mapper cho `dxos-api` |
| Tài khoản và role DX-OS | Dùng hai tài khoản demo được cấp `REQUESTER` và `APPROVER` thủ công | Tạo hoặc ánh xạ account trong môi trường đích và truyền credential qua secret/environment |
| Exactly-once cho action | Chưa thể bảo đảm | DX-OS action API chưa nhận idempotency key; ledger TTCSN chỉ giảm replay trùng |
| Concurrency nguyên tử | Chưa thể bảo đảm | DX-OS action API chưa nhận expected version trong cùng command; preflight hiện tại vẫn có race window |
| Rollback liên hệ thống | Chưa xử lý | External call có thể tạo record DX-OS rồi transaction TTCSN rollback; production cần outbox hoặc reconciliation job |
| Đồng bộ chủ động | Chưa làm | MVP đọc lại DX-OS theo request; chưa có webhook, event checkpoint hoặc consumer |
| Identity theo từng người dùng | Chưa làm | MVP dùng một requester và một approver dùng chung; production cần subject mapping hoặc token delegation |
| Guest checkout ở chế độ DX-OS | Không hỗ trợ | MVP yêu cầu user TTCSN đã đăng nhập để xác định owner |
| Dữ liệu approval cũ | Không migrate | Chỉ cutover trên database sạch hoặc tự xây migration/reconciliation cho dữ liệu local hiện có |
| Hiệu năng danh sách admin | Chưa đo tải | Adapter đang đọc từng record DX-OS theo từng order; cần batch API hoặc cache nếu số order lớn |
| Test riêng cho DX-OS adapter | Chưa thêm mock test chuyên biệt | Phạm vi này dùng live E2E và test hiện có; có thể bổ sung contract test khi DX-OS ổn định contract |
| Hardening production | Chưa thực hiện | Cần secret manager, TLS, rotation, observability, retry policy và cảnh báo lỗi đồng bộ |

## Cấu hình môi trường cần có

Trong container TTCSN, `DXOS_BASE_URL` phải trỏ tới địa chỉ DX-OS mà container truy cập được, ví dụ `http://host.docker.internal:19080` cho demo Windows.

`DXOS_CLIENT_ID` mặc định của cấu hình mẫu là `ttcsn-mvp`.
Client này phải được tạo ở realm `dxos` và token phải chứa audience `dxos-api`.

Credential requester và approver chỉ được cấp qua environment hoặc secret của môi trường triển khai.
Không dùng giá trị placeholder trong `.env.example` cho môi trường thật.

DX-OS tiếp tục là stack riêng.
TTCSN không nhúng database hoặc source DX-OS vào Compose của mình.

## Giới hạn trách nhiệm

TTCSN đã hoàn thành adapter và integration glue ở mức source cho MVP.

Các thiếu hụt về idempotency command và optimistic concurrency nguyên tử thuộc contract action hiện tại của DX-OS.

Các thiếu hụt về Keycloak bootstrap, secret, account mapping, outbox, webhook và quan sát vận hành thuộc phần triển khai production, không phải thay đổi model YAML.

Chỉ nên tuyên bố sẵn sàng production sau khi các mục còn lại ở bảng trên được kiểm tra và xử lý theo môi trường đích.
