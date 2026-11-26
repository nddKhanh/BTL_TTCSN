# Handoff Chuyen — NW-C1/NW-C2

- Trạng thái mục tiêu: commit/push riêng nhánh `Chuyen`, chưa mở PR/merge.
- Tác vụ nhánh: validate auth lifecycle; audit paging và tài liệu điều phối chuyển từ Khánh.
- Auth body thiếu/sai trả 400 `VALIDATION_ERROR` trước service, JSON hỏng trả `MALFORMED_JSON`.
- Email tối đa 254 ký tự; opaque token tối đa 128; mật khẩu reset 8–72 ký tự;
  TOTP 6 chữ số hoặc recovery code 16 ký tự hex. Service vẫn kiểm tra tính hợp lệ thực tế.
- API mới: `GET /api/v1/admin/security-audit/page?eventType=LOGIN&result=SUCCESS&page=0&size=20`.
- Giữ filter `from`/`to` ISO date-time; page >= 0, size 1–100, từ chối from > to.
- Sort giảm dần theo occurredAt rồi id; giữ metadata Page và mask email/IP.
- API list cũ giữ envelope/list nhưng giới hạn 100 sự kiện mới nhất; client cần lịch sử đầy đủ phải dùng endpoint page.
- Kiểm tra: `backend/mvnw.cmd test`, gồm MockMvc auth và audit pagination/boundaries.
- Chưa thay giao diện; không có browser QA để khai báo trong gói này.
- Phụ thuộc đã có ở base: các service auth lifecycle, repository audit và method security.
- Trách nhiệm tiếp theo: review thay đổi và chọn thời điểm tạo PR; file của nhánh khác không nằm trong commit này.
