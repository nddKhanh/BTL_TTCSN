# Mộc Nhiên Coffee Order (BTL_TTCSN)

Dự án thực tập cơ sở ngành: website đặt hàng cà phê với thương hiệu, nội dung và dữ liệu minh họa riêng.

---

## 📌 Kiến trúc & Công nghệ

- **Backend (`backend/`)**:
  - Java 17, Spring Boot 4.1.1, Maven Wrapper.
  - Spring Data JPA, Spring Security, JWT, MySQL (`coffee_order_db`).
  - Swagger UI: `http://localhost:8080/swagger-ui.html`.
- **Frontend (`frontend/`)**:
  - HTML5, CSS3 thuần, JavaScript (Vanilla JS).
  - Quản lý giỏ hàng qua `localStorage`.

---

## 🗂️ Cấu trúc Thư mục

```text
.
├── backend/                  # Source code Spring Boot REST API
├── frontend/                 # Giao diện HTML/CSS/JS thuần
├── docs/                     # Tài liệu tiến độ & lộ trình công việc
│   └── roadmap.md            # Kế hoạch chi tiết & tiến độ dự án
├── .env.example              # File cấu hình mẫu
├── .gitignore
└── README.md
```

---

## ✨ Tính năng Hiện có

### Khách hàng (Storefront)
- [x] Xem danh sách món ăn / thức uống theo danh mục (Cà phê, Trà, Freeze, Bánh).
- [x] Tìm sản phẩm theo tên và kết hợp với bộ lọc danh mục.
- [x] Chọn Size (S, M, L) và Topping (Thạch đào, Kem cheese...).
- [x] Giỏ hàng dạng Drawer trượt (thêm, sửa số lượng, xóa món, tự tính tổng tiền).
- [x] Đặt hàng COD (nhập tên, số điện thoại, địa chỉ giao hàng, ghi chú).
- [x] Phí giao hàng 15.000 VNĐ, miễn phí khi tạm tính từ 200.000 VNĐ; tổng tiền do backend xác nhận.
- [x] Tra cứu & theo dõi trạng thái đơn hàng theo mã đơn.
- [x] Đăng ký, đăng nhập JWT và cập nhật hồ sơ khách hàng qua API.

### Quản trị (Admin)
- [x] Xem danh sách toàn bộ đơn hàng mới đặt.
- [x] Cập nhật trạng thái đơn (Chờ xác nhận, Đã xác nhận, Đang giao, Hoàn thành, Hủy).
- [x] Endpoint quản trị đơn hàng được bảo vệ bởi quyền `ADMIN`.
- [x] CRUD danh mục và sản phẩm qua REST API; sản phẩm được ẩn thay vì xóa khỏi dữ liệu đơn hàng.

---

## ⚡ Hướng dẫn Khởi chạy

### 1. Backend
Thiết lập `DB_URL`, `DB_USERNAME` và `DB_PASSWORD` trong `.env` theo MySQL đang chạy.
Thiết lập `JWT_SECRET` là chuỗi Base64 mã hóa từ ít nhất 32 byte trước khi chạy backend.

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

### 2. Frontend
Mở trực tiếp file `frontend/index.html` trên trình duyệt hoặc qua Live Server.
Trang quản trị: `frontend/admin.html`.

---

*Xem chi tiết kế hoạch các bước và tiến độ công việc tại [docs/roadmap.md](docs/roadmap.md).*
