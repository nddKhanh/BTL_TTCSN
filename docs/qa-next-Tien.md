# Handoff Tien — NW-T1/NW-T2

- Phạm vi: refresh/session (SA08/SA12) và phần authorization QA chuyển từ Khánh (SA10/SA11).
- Refresh lookup dùng pessimistic write lock để serialize hai lần sử dụng cùng token.
- Khi reuse/expired token bị từ chối, các session còn hoạt động trong family bị thu hồi.
  `BadCredentialsException` trả lỗi xác thực và không rollback phần thu hồi.
- Account không ACTIVE, đang khóa hoặc token tạo trước lần đổi mật khẩu không được refresh;
  thu hồi session user trong nhánh kiểm tra trạng thái.
- Token rỗng/quá dài/không tồn tại có cùng thông báo lỗi, không cấp token mới.
- Test Spring transaction interceptor chứng minh replay được commit thay vì rollback.
  Test này không thay thế kiểm tra concurrency trên MySQL thật.
- Sửa thứ tự đăng ký filter: JWT trước, sau đó đặt rate-limit trước JWT; context HTTP khởi tạo thành công.
- Logout-all và các route MFA cần xác thực; method permission bị từ chối trả 403 `ACCESS_DENIED`, không trả 500.
- MockMvc chạy SecurityFilterChain thật và method-security proxy thật; JWT parsing/user lookup dùng mock.
- Ma trận audit: anonymous 401; CUSTOMER 403; ADMIN thiếu REPORT_READ 403; ADMIN có REPORT_READ 200.
- Giữ mô hình CUSTOMER/ADMIN hiện tại; chưa thêm API đổi role hay STAFF/MANAGER trong gói này.
- Kiểm tra: full `backend/mvnw.cmd test`, `git diff --check`, dry merge main trước push.
- Trạng thái mục tiêu: pushed trên `Tien`; chưa mở PR/merge. Người review quyết định bước tích hợp tiếp theo.
