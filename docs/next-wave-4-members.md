# Đợt hoàn thiện P0/P1 sau đồng bộ — 04/11/2026

## Phạm vi và phân công

Base chung: `91753e2970d067cc4c8664412c8ebdf7f40813be` của repo clone.
Đợt này hoàn thiện các khoảng trống có thể chứng minh trong code của hai phase hiện tại;
không làm lại các commit PR01–PR15/SA01–SA15 đã có và chưa bắt đầu RAG.
Theo yêu cầu chủ repo, chỉ commit/push lên nhánh được giao, chưa mở PR hoặc merge.

Chủ tài khoản thao tác là `nddKhanh`, dùng danh tính Git đã cấu hình và phiên CLI do chủ
tài khoản trực tiếp đăng nhập. Tên nhánh là phạm vi phân công, không phải bằng chứng
người cùng tên đã tự thực hiện thay đổi. Không đổi author hay tạo commit rỗng.

Mỗi nhánh nhận một gói chính (4 điểm ước lượng) và một gói từ phần còn thiếu của
Khánh (4 điểm). Đây là cân bằng khối lượng ước lượng, không chia commit để làm đều
lịch sử. Docker/CI nền và sơ đồ đã có; không tạo lại chúng cho đủ chỉ tiêu.

| Nhánh | Gói chính | Gói chuyển từ Khánh | Nghiệm thu |
|---|---|---|---|
| `Chuyen` | NW-C1: validate body verify/reset/refresh/email/MFA, tiếp nối SA13/API contract | NW-C2: truy vấn audit có giới hạn, tiếp nối SA15; điều phối tài liệu QA PR15 | Body sai trả 400 trước service; audit có page/size/sort ổn định, từ chối khoảng ngày sai |
| `Tien` | NW-T1: refresh rotation, reuse/revocation và trạng thái tài khoản, tiếp nối SA08/SA12 | NW-T2: ma trận route/method authorization thật, tiếp nối SA10/SA11 | Reuse thu hồi family vẫn commit khi trả lỗi; request không xác thực 401, thiếu quyền 403, đúng quyền thành công |
| `Cuong` | NW-U1: owner tra cứu đơn bằng tài khoản, guest vẫn cần số điện thoại, tiếp nối PR03 | NW-U2: phân trang đơn quản trị backend, tiếp nối PR10 | Không lộ đơn cho tài khoản khác; truy vấn page có filter/sort/giới hạn và không fetch collection trong page query |
| `Bach` | NW-B1: refresh dùng chung một promise và retry tối đa một lần, tiếp nối SA09 | NW-B2: điều hướng trang sản phẩm quản trị, tiếp nối PR10 frontend | Request đồng thời chỉ rotate một lần; trang trước/sau có bounds, giữ được dữ liệu trang khi thao tác và chống kết quả cũ ghi đè |

## Commit dự kiến và file được phép

### Chuyen — NW-C1, NW-C2

1. `docs(plan): allocate the next hardening wave across four branches` — tài liệu này.
2. `fix(auth): validate lifecycle request bodies` — `AuthController.java`, test controller mới.
3. `test(auth): cover invalid and valid lifecycle requests` — test controller, contract nếu cần.
4. `feat(audit): add bounded paginated audit queries` — `SecurityAuditService.java`, `AdminSecurityAuditController.java`, test service.
5. `test(audit): verify pagination and invalid query boundaries` — test mới hoặc mở rộng, tài liệu QA riêng nhánh.

### Tien — NW-T1, NW-T2

1. `fix(auth): commit refresh family revocation on token reuse` — `RefreshSessionService.java`, `RefreshSessionRepository.java`, test service mới.
2. `test(auth): cover refresh rotation and account rejection` — test service/transaction mới, tối đa 3 file.
3. `fix(authz): enforce endpoint permission boundaries` — security config/policy/exception mapping, tối đa 3 file.
4. `test(authz): exercise HTTP authentication and permission matrix` — integration-style MockMvc test, tài liệu QA riêng nhánh.

### Cuong — NW-U1, NW-U2

1. `fix(order): allow authenticated owners to look up their orders` — `OrderService.java`, `OrderController.java`, test lookup mới.
2. `test(order): cover guest and cross-account lookup boundaries` — test lookup/controller, tối đa 3 file.
3. `feat(admin): add filtered server-side order pagination` — repository, service, controller, tối đa 3 file.
4. `test(admin): verify order paging filters and bounds` — test service/controller, tài liệu QA riêng nhánh, tối đa 3 file.

### Bach — NW-B1, NW-B2

1. `fix(ui): coordinate concurrent authentication refreshes` — `frontend/js/api.js`, test Node mới.
2. `test(ui): cover refresh failures and retry limits` — test Node, tối đa 3 file.
3. `feat(admin-ui): navigate server-side product pages` — `frontend/js/admin.js`, `frontend/admin.html`, test Node mới.
4. `test(admin-ui): cover page bounds and stale responses` — test Node, tài liệu QA riêng nhánh.

Tên file đầy đủ theo package `backend/src/{main,test}/java/com/highlands/order/`.
Các test mới có phạm vi riêng để không xung đột giữa các nhánh. Nếu cần tách thêm một
commit để mỗi trạng thái đều chạy được, cập nhật kế hoạch trước khi stage; không tạo
commit test chỉ lặp lại implementation hoặc cố ép đúng số lượng commit.

## Phụ thuộc và kiểm tra

- Mọi gói dựa trên dependency đã có trong main; các nhánh không cherry-pick công việc nhau.
- Product pagination UI dùng endpoint `/admin/products/page` đã có ở base. Order paging
  backend của Cuong chưa được nối vào UI ở đợt này để giữ độc lập giữa hai nhánh.
- Giữ endpoint cũ tương thích; endpoint page mới trả Spring `Page` trong envelope API hiện tại.
- Từng commit: stage đường dẫn tường minh, `git diff --cached --check`, 1–3 file.
- Từng nhánh: full backend test nếu có Java; Node test nếu có JS; diff check;
  dry merge main; kiểm tra mọi file đều thuộc phạm vi được giao và dưới 20 file.
- Trước push: fetch lại main và nhánh đích, từ chối ghi đè commit bên ngoài, xác nhận
  identity CLI vẫn là nddKhanh. Push thường; giữ nguyên main và repo chính.
- Kiểm tra phối hợp trên worktree riêng không push: kết hợp cả bốn nhánh, chạy lại test
  để phát hiện lỗi giao tiếp. Kết quả này không biến công việc nhánh khác thành commit của nhánh mình.

## Ngoài đợt này

Các phần roadmap vẫn cần đợt riêng: MySQL/Testcontainers và kiểm tra migration thật;
SMTP/outbox bền vững; refresh HttpOnly cookie + access token memory; MFA pre-auth
thay vì challenge sau access token; session management UI; bảo vệ admin cuối khi có
API đổi role; audit retention; order timeline/locking; hồ sơ/địa chỉ; báo cáo nâng cao.
Không đánh dấu toàn bộ hai phase hoàn tất chỉ vì các khung chức năng đã tồn tại.
