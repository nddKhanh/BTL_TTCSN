# Handoff Bach — NW-B1/NW-B2

- Phạm vi: refresh client (SA09) và điều hướng product paging UI chuyển từ Khánh PR10.
- Dùng chung một promise refresh cho request đồng thời; phản hồi 401 đến muộn dùng token
  vừa rotate. Mỗi request chỉ retry một lần; login/register/refresh và lifecycle public không tự refresh.
- Refresh lỗi xóa đúng auth keys, giữ cart; kết quả refresh cũ không ghi đè logout/login mới.
- HTTP 204 thành công trả null; logout-all không xóa dữ liệu local khác.
- Product admin dùng endpoint page đã có ở base, size 20. Nút trước/sau có bounds,
  disabled khi loading; trạng thái trang có live region và danh sách có aria-busy.
- Giữ trang khi reload/cập nhật; thêm mới quay trang đầu. Trang cuối biến mất thì tải trang hợp lệ cuối.
- Request mới thắng request cũ; lỗi có thông báo và nút thử lại; dữ liệu text sản phẩm được escape.
- Test tự động: `node --test frontend/tests/*.test.cjs`; kiểm tra cú pháp hai JS bằng `node --check`.
- Harness Node chạy JS thật với fetch/storage/DOM tối thiểu giả; chưa thay thế QA layout trong browser.
- Browser/viewport chưa chạy trong phiên này; cần review Chrome/Edge tại 360/768/1024/1440 px
  cho nút pager, focus bàn phím, loading/error/empty và trạng thái lưu sau sửa sản phẩm.
- Giữ localStorage token hiện tại để không đổi contract với backend ở nhánh khác; chuyển
  HttpOnly refresh cookie/token memory là gói riêng theo roadmap.
- Mục tiêu: pushed riêng `Bach`, chưa PR/merge. Không chứa thay đổi endpoint order page của Cuong.
