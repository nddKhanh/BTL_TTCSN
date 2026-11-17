# PoF tích hợp Dynamic Core DX-OS

## Mục tiêu

PoF kiểm chứng DX-OS như một dịch vụ độc lập mà TTCSN gọi qua API công khai.
Domain bán hàng, định giá và fulfillment của TTCSN không phụ thuộc mã nguồn hoặc cơ sở dữ liệu DX-OS.

PoF trả lời riêng ba câu hỏi:

1. Một tệp model có tạo được schema, quyền, API và workflow dùng được không.
2. Đổi cùng tệp model có thay đổi được schema và topology mà không sửa mã TTCSN không.
3. `DxosApprovalAdapter` có thay `LocalApprovalAdapter` mà không đổi controller, domain và frontend không.

Ba tệp trong `models/` là ba phiên bản liên tiếp của cùng model logic `ttcsn_order_approval`.
Chúng được giữ riêng để phép thử có thể lặp lại và so sánh bằng chứng; khi vận hành, người triển khai chỉ áp dụng một phiên bản tại một thời điểm.

## Ranh giới bằng chứng

- Chỉ dùng Models API, Meta API, Data API, action API và Events API công khai.
- Không đọc trực tiếp PostgreSQL, Flowable, Keycloak database hoặc object storage của DX-OS.
- Harness không tự duy trì state machine và không tự suy ra action hợp lệ.
- Bằng chứng phải lưu request, response, model version/hash, apply run, event và kết quả sau restart.
- UI là tùy chọn; API runtime là bằng chứng chính.

## Các phiên bản model

- `v1.yaml`: approval một bước với `approve`, `reject` và `withdraw`.
- `v2-schema.yaml`: đổi tên trường mở rộng có bảo toàn dữ liệu và thêm trường bắt buộc có default.
- `v3-topology.yaml`: thêm bước review trước khi quyết định.

V3 chỉ được apply sau khi mọi record của V1/V2 đã ở trạng thái cuối.
Nếu DX-OS chấp nhận đổi topology trong khi còn record mở, PoF phải ghi nhận đó là lỗi an toàn thay vì tiếp tục.

Các trường `order_id`, `order_code`, `requester_name` và `total_amount` là contract giữa TTCSN với model.
PoF không đổi tên chúng vì đó sẽ là breaking API đối với mọi ứng dụng ngoài, không phải bằng chứng về khả năng đổi nội bộ của Dynamic Core.

## Môi trường cô lập

`docker-compose.pof.yml` được ghép với Compose chính của DX-OS.
Nó thay toàn bộ thư mục dữ liệu, model và webhook bằng đường dẫn thuộc `.local/dxos-pof/` của TTCSN.
Không dùng hoặc xóa container, volume và dữ liệu của stack DX-OS đang có.

## Trạng thái hiện tại

PoF V1 → V2 → V3 đã chạy qua API công khai trên một stack DX-OS cô lập.
Kết quả xác nhận schema và topology thay đổi theo model mà không sửa mã ứng dụng thử nghiệm.
DX-OS đã chặn thay topology khi còn record mở và giữ version, hash, record cùng event sau restart.

TTCSN chưa chuyển `APPROVAL_PROVIDER` sang DX-OS.
Action API hiện tại của DX-OS chưa nhận idempotency key hoặc expected version, nên chưa thể bảo toàn đầy đủ contract retry và concurrency của `ApprovalPort`.
Chi tiết request, kết quả và ranh giới trách nhiệm được ghi tại [kết quả PoF](RESULTS.md).
