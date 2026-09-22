# Lộ trình & Kế hoạch Chi tiết Dự án (Roadmap)

Tài liệu mô tả chi tiết các phân hệ chức năng, các bước triển khai, phân quyền người dùng và tiến độ thực hiện của dự án **Highlands Coffee Order Clone**.

---

## 📊 Tổng quan Tiến độ Các Phân Hệ

| Phân hệ | Nội dung chính | Trạng thái |
| :--- | :--- | :--- |
| **1. Khung cơ bản & Storefront** | Menu, Modal chọn món, Giỏ hàng, Đặt hàng COD | `Hoàn thành` |
| **2. Xác thực & Phân quyền (Auth)** | Đăng ký, Đăng nhập, Phân quyền Khách / Admin | `Đang thực hiện` |
| **3. Quản lý Sản phẩm (Admin)** | CRUD Danh mục, Món ăn, Size, Topping | `Chưa bắt đầu` |
| **4. Xử lý Đơn hàng & Lịch sử** | Luồng xử lý đơn, Xem lịch sử đơn cá nhân | `Chưa bắt đầu` |
| **5. Thống kê & Báo cáo Admin** | Doanh thu, Đếm đơn theo trạng thái | `Chưa bắt đầu` |
| **6. Tinh chỉnh UI/UX & Responsive** | Tìm kiếm, Lọc giá, Toast thông báo, Mobile UI | `Chưa bắt đầu` |
| **7. Kiểm thử & Tài liệu Báo cáo** | Test API, Viết báo cáo TTCSN | `Chưa bắt đầu` |

---

## 📝 Kế hoạch Triển khai Chi tiết Từng Chức Năng

### Module 1: Khung Dự án & Storefront Cơ bản (Đã xong)
- [x] **Backend**: Cấu hình Spring Boot 3.3.0, Spring Data JPA, kết nối MySQL (`highlands_db`).
- [x] **Data Seeder**: Nạp sẵn danh mục mẫu, sản phẩm (Phin sữa, Trà sen vàng...), size (S/M/L) và topping.
- [x] **Storefront UI**: Trang chủ hiển thị danh mục, danh sách món kèm ảnh và giá cơ bản.
- [x] **Modal Chọn món**: Tùy chọn Size, Topping, tăng giảm số lượng, tự động tính tổng tiền.
- [x] **Giỏ hàng Drawer**: Lưu trữ qua `localStorage`, tăng/giảm số lượng, xóa món.
- [x] **Checkout đơn giản**: Form điền tên, SĐT, địa chỉ nhận hàng và gửi đơn về backend.
- [x] **Trang trạng thái đơn**: Tra cứu đơn hàng vừa đặt qua mã đơn (`order-status.html`).

---

### Module 2: Xác thực & Phân quyền (Authentication & Authorization)

#### 1. Backend (Spring Security / JWT)
- [ ] Thiết kế Entity `User` (`id`, `username`, `password_hash`, `full_name`, `phone`, `role` [ROLE_CUSTOMER, ROLE_ADMIN], `created_at`).
- [ ] API Đăng ký tài khoản: `POST /api/v1/auth/register` (Validate trùng username/phone, mã hóa password bằng BCrypt).
- [ ] API Đăng nhập: `POST /api/v1/auth/login` (Xác thực và trả về Token + Thông tin User).
- [ ] API Lấy thông tin cá nhân: `GET /api/v1/auth/me`.
- [ ] Phân quyền bảo vệ Endpoint:
  - **Public**: Xem menu, tìm kiếm sản phẩm, đặt hàng không cần tài khoản, xem trạng thái đơn qua mã.
  - **ROLE_CUSTOMER**: Xem lịch sử các đơn hàng đã đặt của tài khoản, cập nhật thông tin cá nhân.
  - **ROLE_ADMIN**: Toàn quyền thêm/sửa/xóa menu, quản lý đơn hàng, xem thống kê.

#### 2. Frontend
- [ ] Tạo trang Đăng nhập (`login.html`) và Đăng ký (`register.html`).
- [ ] Xử lý lưu Token và thông tin User vào `localStorage`.
- [ ] Cập nhật Header:
  - Chưa đăng nhập: Hiện nút **Đăng nhập / Đăng ký**.
  - Đã đăng nhập: Hiện **Tên người dùng**, menu dropdown **Lịch sử đơn**, **Đăng xuất**.
- [ ] Guard bảo vệ trang `admin.html`: Kiểm tra quyền `ROLE_ADMIN`, nếu chưa đăng nhập hoặc không phải admin thì chuyển hướng về `login.html`.

---

### Module 3: Quản lý Danh mục & Sản phẩm (Admin CMS)

#### 1. Backend
- [ ] `POST /api/v1/admin/products`: Thêm món mới kèm ảnh, giá gốc, danh mục và các sizes.
- [ ] `PUT /api/v1/admin/products/{id}`: Sửa thông tin món, giá và cấu hình size/topping.
- [ ] `DELETE /api/v1/admin/products/{id}`: Đổi trạng thái `is_active = false` (ẩn món thay vì xóa cứng).
- [ ] `POST / PUT / DELETE /api/v1/admin/categories`: Thêm / sửa / xóa danh mục món.
- [ ] `POST / PUT / DELETE /api/v1/admin/toppings`: Thêm / sửa giá topping.

#### 2. Frontend (`admin.html`)
- [ ] Thêm Tab **Quản lý Thực đơn (Products)**:
  - Bảng danh sách món ăn, danh mục, trạng thái (Đang bán / Tạm ẩn).
  - Modal Form thêm món mới (Nhập tên, chọn danh mục, giá, URL ảnh, chọn sizes hỗ trợ).
  - Nút sửa món và nút bật/tắt trạng thái hiển thị.
- [ ] Thêm Tab **Quản lý Topping & Danh mục**: Thêm nhanh topping mới và sửa giá.

---

### Module 4: Xử lý Đơn hàng & Quản lý Lịch sử Đơn

#### 1. Khách hàng
- [ ] Tích hợp `userId` vào đơn hàng khi người dùng đã đăng nhập đặt món.
- [ ] Trang **Lịch sử đơn hàng** (`my-orders.html`): Xem danh sách các đơn đã đặt của tài khoản kèm trạng thái chi tiết.
- [ ] Cho phép khách hàng Hủy đơn khi đơn vẫn ở trạng thái `PENDING` (Chờ xác nhận).

#### 2. Quản trị viên (Admin)
- [ ] Bộ lọc đơn hàng trên `admin.html`: Lọc theo trạng thái (Chờ duyệt, Đang giao, Hoàn tất, Đã hủy), lọc theo ngày đặt.
- [ ] Tìm kiếm đơn theo Tên khách hàng hoặc Số điện thoại.
- [ ] Modal xem chi tiết món ăn trong đơn hàng của Admin.

---

### Module 5: Thống kê & Báo cáo Cơ bản (Admin Analytics)
- [ ] Backend API: `GET /api/v1/admin/stats` (Tính tổng doanh thu, tổng số đơn theo ngày/tháng, số đơn đang chờ xử lý).
- [ ] Frontend: Thêm các thẻ tóm tắt (Cards) trên Dashboard Admin:
  - 💰 Tổng doanh thu
  - 📦 Số đơn hoàn thành
  - ⏳ Số đơn đang chờ duyệt
  - ☕ Món bán chạy nhất

---

### Module 6: Tối ưu UI/UX & Responsive
- [ ] Thanh tìm kiếm sản phẩm: Lọc món theo từ khóa gõ vào (Live search).
- [ ] Tùy chọn độ ngọt / đá (% đá, % đường) trên Modal chọn món.
- [ ] Hệ thống Toast thông báo (Thêm vào giỏ thành công, Đặt hàng thành công, Thông báo lỗi).
- [ ] Tối ưu CSS Responsive cho màn hình điện thoại (Mobile menu, Giỏ hàng full-width).

---

### Module 7: Kiểm thử & Hoàn thiện Báo cáo
- [ ] Viết kịch bản kiểm thử (Test cases) luồng đặt hàng và quản trị.
- [ ] Xuất tài liệu đặc tả API hoàn chỉnh qua Swagger UI.
- [ ] Soạn thảo báo cáo môn học TTCSN (Sơ đồ Use Case, ERD, Kiến trúc hệ thống, Giao diện thực tế).
- [ ] Chuẩn bị kịch bản demo sản phẩm.
