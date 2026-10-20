# Lộ trình phát triển Mộc Nhiên Coffee Order

> Tài liệu này là nguồn kế hoạch duy nhất của dự án từ ngày 2026-10-20.
> Kế hoạch cũ ở thư mục gốc đã được thay bằng bộ tài liệu trong `docs/` để tránh hai checklist mâu thuẫn.

## 1. Hiện trạng đã xác minh

- Backend Spring Boot 4.1.1, Java 17, Maven Wrapper, JPA, MySQL, Flyway, Spring Security và JWT đã hoạt động.
- Frontend HTML/CSS/Vanilla JS đã có catalog, giỏ hàng, checkout, tra cứu đơn, lịch sử đơn và trang quản trị.
- Luồng chính đã có đăng ký/đăng nhập, coupon, tùy chọn món, tạo/hủy/cập nhật đơn và dashboard cơ bản.
- API đã dùng response DTO và envelope lỗi; Swagger dependency đã được khai báo.
- Repository hiện có 16 test method, nhưng còn thiếu controller/integration/E2E test và kiểm tra migration trên MySQL thật.
- Rủi ro lớn nhất hiện tại là bảo mật tra cứu đơn public, độ phủ test, phân trang/truy vấn báo cáo, cấu hình production và QA trước bảo vệ.

## 2. Mục tiêu phát hành

| Mốc | Mục tiêu | Điều kiện hoàn thành |
| --- | --- | --- |
| `v0.2 Stabilize` | Mã lỗi nghiệp vụ, security route, tra cứu đơn an toàn, test giá/coupon, OpenAPI | Test xanh; customer không truy cập admin; không tra cứu đơn chỉ bằng mã ngắn |
| `v0.3 Storefront` | Sort/pagination, URL filter, cart UX, checkout validation, accessibility | Luồng mobile/desktop ổn định; giá luôn được backend xác nhận |
| `v0.4 Operations` | Phân trang admin, dashboard aggregate và CSV sau khi có nhu cầu | Số liệu khớp DB; không tải toàn bộ order để tính báo cáo |
| `v0.5 Delivery` | Docker, CI, secret scan, healthcheck, tài liệu vận hành | Có thể dựng môi trường sạch và chạy smoke test lặp lại |
| `v1.0 Defense` | ERD, kiến trúc, API docs, checklist QA và kịch bản demo | Không còn lỗi blocker/high; thành viên giải thích được phần mình đóng góp |

## 3. Thứ tự triển khai đã cam kết

### P0 — Ổn định và bảo mật

- [ ] Chuẩn hóa mã lỗi nghiệp vụ cho auth, order, coupon và catalog.
- [ ] Rà soát route public/customer/admin và bổ sung security test.
- [ ] Bảo vệ tra cứu đơn guest bằng mã đơn kết hợp số điện thoại hoặc lookup token.
- [ ] Hoàn thiện test tính giá, phí giao hàng, coupon và chuyển trạng thái.
- [ ] Hoàn thiện mô tả OpenAPI cho auth, catalog, order và admin.
- [ ] Thêm security headers; rà soát `innerHTML`, secret, dữ liệu demo và log nhạy cảm.

### P1 — Trải nghiệm khách hàng

- [ ] Sort và pagination catalog ở backend; đồng bộ bộ lọc lên URL.
- [ ] Thêm loading, empty, retry state và thông báo lỗi nhất quán.
- [ ] Cho sửa tùy chọn dòng hàng, gộp dòng giống nhau và hiển thị tiến độ miễn phí giao hàng.
- [ ] Validate số điện thoại/địa chỉ/ghi chú ở cả frontend và backend.
- [ ] Cải thiện keyboard navigation, focus, ARIA và độ tương phản.
- [ ] Sau gói 30 commit: hồ sơ khách hàng, nhiều địa chỉ, timeline đơn và đặt lại đơn.

### P1 — Quản trị và báo cáo

- [ ] Phân trang server-side cho bảng đơn và sản phẩm.
- [ ] Chuyển dashboard sang aggregate query theo khoảng ngày.
- [ ] Bổ sung top sản phẩm, tỷ lệ hủy và giá trị đơn trung bình sau khi số liệu nền ổn định.
- [ ] Thêm audit người cập nhật trạng thái và optimistic locking cho Order ở vòng sau.
- [ ] Chỉ thêm export CSV khi bộ lọc/phân trang đã có contract ổn định.

### P1 — Chất lượng và bàn giao

- [ ] Dockerfile multi-stage, Compose local và healthcheck tối thiểu.
- [ ] GitHub Actions chạy test, `git diff --check` và secret scan trên PR.
- [ ] Kiểm tra migration trên database mới và database có dữ liệu sao lưu.
- [ ] Hoàn thiện ERD, sơ đồ kiến trúc, kịch bản demo và checklist QA.
- [ ] Chạy Chrome/Edge ở 360, 768, 1024 và 1440 px.

### P2 — Chưa cam kết

- Recommendation baseline, sau đó mới đánh giá semantic retrieval/RAG.
- Upload ảnh, email, thanh toán sandbox, tích điểm, PWA và đa cửa hàng.
- Không chuyển microservices hoặc rewrite frontend trước khi P0/P1 hoàn tất.

## 4. Gói thực thi hiện tại

Gói tiếp theo gồm 15 PR và 30 commit nguyên tử trong [danh mục work package](work-packages.md).

- Phương án 3 thành viên: [team-plan-3-members.md](team-plan-3-members.md).
- Phương án 5 thành viên: [team-plan-5-members.md](team-plan-5-members.md).
- Quy trình nhánh/commit/PR: [git-workflow.md](git-workflow.md).
- Mẫu nhận việc và báo cáo: [templates/git-task-template.md](templates/git-task-template.md).

Không chạy đồng thời cả hai phương án phân công.
Chọn đúng một phương án trước khi tạo issue đầu tiên.

## 5. Definition of Ready

Một việc chỉ được nhận khi có:

- mục tiêu và tiêu chí nghiệm thu rõ;
- người thực hiện thật và người review;
- danh sách file dự kiến;
- tối đa 3 file cho mỗi commit;
- tối đa 19 file thay đổi cho toàn PR;
- dependency đã merge hoặc được ghi rõ;
- lệnh test/kiểm tra có thể chạy lại.

## 6. Definition of Done

- Nhánh được tạo từ `main` mới nhất.
- Mỗi commit có một mục đích, không quá 3 file và dùng Conventional Commits.
- Không dùng commit rỗng, đổi ngày, sửa `--author` hoặc chia thay đổi giả tạo chỉ để tăng số commit.
- Đã fetch và merge `origin/main` vào nhánh để phát hiện conflict trước PR.
- Test liên quan, test toàn backend khi phù hợp và `git diff --check` đều đạt.
- PR có dưới 20 file thay đổi, mô tả cách test và rủi ro.
- Người review khác người tạo PR; tác giả tự giải thích được thay đổi.
- Sau merge, issue/checklist và tài liệu liên quan được cập nhật.

## 7. Sau gói 30 commit

Ưu tiên kế tiếp được chọn từ dữ liệu QA, không chỉ từ số lượng tính năng:

1. Integration test MySQL/Testcontainers và kiểm tra Flyway.
2. Audit trạng thái đơn, optimistic locking và lịch sử thay đổi.
3. Hồ sơ, địa chỉ, quên mật khẩu và thông báo trạng thái.
4. Dashboard nâng cao và CSV.
5. Recommendation baseline không AI; chỉ làm RAG khi baseline có metric và privacy review.
