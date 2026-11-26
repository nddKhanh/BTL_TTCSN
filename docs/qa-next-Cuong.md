# Handoff Cuong — NW-U1/NW-U2

- Phạm vi: owner/guest order lookup và phần phân trang đơn backend chuyển từ Khánh PR10.
- `GET /api/v1/orders/{code}`: principal do JWT/security resolver cung cấp, không nhận email từ client.
- Đúng chủ đơn (email không phân biệt hoa thường) có thể bỏ phone; guest hoặc tài khoản khác
  vẫn phải có đúng phone. Đơn guest không tự nhận owner chỉ vì client đã đăng nhập.
- Giữ API overload cũ yêu cầu phone và giữ lỗi not-found cho mã không tồn tại.
- API mới: `GET /api/v1/orders/admin/page?page=0&size=20&status=COMPLETED&keyword=COF`.
- Có filter `createdFrom`/`createdTo` ISO date; ngày cuối được chuyển thành cận trên loại trừ ngày kế tiếp.
- Page >= 0, size 1–100; từ chối status/khoảng ngày sai. Sort createdAt DESC, id DESC ổn định.
- Query paginated không fetch collection; count query dùng cùng filter. Mapping DTO diễn ra
  trong transaction service, lazy items có thể tạo thêm query trong phạm vi page tối đa 100 đơn.
- API `/admin/all` cũ giữ tương thích; kết nối order page vào UI là gói tiếp theo sau review backend.
- Test: owner/guest/cross-account boundaries, repository argument/metadata, MockMvc endpoint envelope/bounds.
- Chưa chạy MySQL thật trong gói này; smoke SQL/Flyway là backlog riêng đã ghi trong kế hoạch.
- Trạng thái mục tiêu: pushed riêng `Cuong`, chưa PR/merge; review theo kế hoạch trước tích hợp.
