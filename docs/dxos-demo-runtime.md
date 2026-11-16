# Nhật ký kiểm chứng runtime và demo DX-OS

Tài liệu này là nguồn trạng thái cho việc dựng và kiểm tra demo TTCSN ↔ DX-OS.
Không lặp lại các kiểm tra đã ghi **Đã xác minh** trừ khi commit, model, cấu hình hoặc dữ liệu runtime thay đổi.

## Mốc kiểm tra 2026-11-10

### Đã xác minh

- Docker Desktop hoạt động; Docker Engine `29.8.2` trả lời bình thường.
- Stack DX-OS dự án `dxos-dev` ở `http://localhost:9080` đã healthy cho PostgreSQL, Keycloak, backend, APISIX, frontend, portal, monitor và SeaweedFS.
- Stack `dxos-dev` chạy source commit `56d9ce8e168b4586d36cdd2a7087a6ee9580de72` trên nhánh làm việc `feat/dagre-integration`.
- Commit PoF được tài liệu hóa là `5861c8bf5fb1e1c76c3b60a0ddb9e36d09f52c3c`; commit này không phải ancestor của commit đang chạy ở `dxos-dev`.
- Models API của stack `9080` từ chối `docs/dxos-pof/models/v3-topology.yaml` với `ERR_MODEL_INVALID`: schema runtime chỉ nhận process family có sẵn và không nhận khóa `states`/`actions` của topology động.
- Vì vậy không dùng stack `9080` để kết luận về demo topology V3. Demo TTCSN phải dùng đúng commit PoF `5861c8b` hoặc một commit mới hơn có contract tương thích đã được kiểm tra lại.
- Worktree tạm detached tại commit PoF được đặt ở `.local/dxos-pof-src`; không đổi branch hoặc file của repo DX-OS đang dùng.
- Stack PoF cách ly dùng project `ttcsn-dxos-pof`, gateway `http://localhost:19080`, PostgreSQL host port `55433` và image prefix `ttcsn-pof`; không dùng dữ liệu/port của stack `dxos-dev`.
- `.env` local của TTCSN dùng `APPROVAL_PROVIDER=dxos`, `DXOS_BASE_URL=http://host.docker.internal:19080`, model `ttcsn_order_approval`, entity `approval` và client `ttcsn-mvp`. File `.env` bị Git bỏ qua và không được commit.

### Đang chờ xác minh trong phiên này

- Image backend/frontend của stack PoF build thành công và toàn bộ container healthy.
- Model `v3-topology.yaml` tự apply thành công trên stack PoF sạch.
- Keycloak PoF có client public `ttcsn-mvp`, direct access grant, audience `dxos-api`, hai user demo và role `REQUESTER`/`APPROVER`.
- Stack TTCSN ở `http://localhost:3000` healthy khi dùng provider DX-OS.
- Browser QA xác nhận nút `Tiếp nhận`, `Duyệt`, `Từ chối`, yêu cầu lý do và trạng thái sau refresh.

## Quy tắc cập nhật

Sau mỗi lần kiểm tra, chỉ chuyển một mục sang **Đã xác minh** khi có lệnh/API/UI evidence thực tế.
Nếu kết quả phụ thuộc commit hoặc dữ liệu mới, thêm một mốc ngày mới thay vì sửa mất bằng chứng cũ.
Không ghi mật khẩu, token, client secret hoặc JWT secret vào tài liệu này.

## Trạng thái phiên khởi chạy 2026-11-10

- DX-OS PoF được build từ worktree cô lập và khởi động thành công với project `ttcsn-dxos-pof`; gateway `http://localhost:19080` trả HTTP 200. Các container backend, Keycloak, frontend, portal, monitor, APISIX, PostgreSQL và SeaweedFS đều báo healthy trong lần `docker compose up --wait`.
- Cấu hình APISIX PoF đã được sửa cục bộ: đường dẫn output phải là thư mục chứa tệp `apisix.yaml`. Lúc tạo cấu hình Docker tự tạo nhầm một thư mục `apisix.yaml`; đã thay bằng tệp rỗng để bước sinh cấu hình hoàn tất.
- TTCSN đã build thành công. Lần đầu không khởi động được MySQL do cổng 3306 đang được tiến trình khác sử dụng; `.env` cục bộ chuyển `MYSQL_HOST_PORT` sang `3307`.
- MySQL TTCSN đã healthy. Tuy nhiên Docker Desktop sau đó trả `500 Internal Server Error` khi kiểm tra/log container backend TTCSN; `http://localhost:3000` không phản hồi trong lần kiểm tra cuối. Vì vậy chưa xác nhận TTCSN healthy, model V3 được apply, hay luồng demo chạy hết. Chưa hướng dẫn người dùng test nghiệp vụ cho đến khi khôi phục được Docker Engine và xác nhận backend.
- Không tắt stack `dxos-dev` đang chạy ở cổng 9080. Demo PoF dùng cổng 19080 riêng.
- Kiểm tra tiếp theo sau đó cho thấy tiến trình giao diện Docker Desktop vẫn còn, nhưng Docker CLI nhận HTTP 500 từ named pipe `dockerDesktopLinuxEngine` tại `/version` và `/info`; lệnh `docker info` không lấy được ServerVersion. Cổng `19080`, `3000` và `3307` đều không lắng nghe ở lần kiểm tra này. Điều này xác nhận Engine/API đang lỗi, không chỉ ứng dụng backend TTCSN.
