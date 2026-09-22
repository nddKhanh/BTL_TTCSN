# Highlands Coffee Order Clone (BTL_TTCSN)

Dự án thực tập cơ sở ngành: Website đặt hàng trực tuyến phỏng theo [Highlands Coffee Order](https://order.highlandscoffee.com.vn/).

---

## 📌 Kiến trúc & Công nghệ

- **Backend (`backend/`)**:
  - Java 17, Spring Boot 3.3.0.
  - Spring Data JPA, MySQL (`highlands_db`).
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
- [x] Chọn Size (S, M, L) và Topping (Thạch đào, Kem cheese...).
- [x] Giỏ hàng dạng Drawer trượt (thêm, sửa số lượng, xóa món, tự tính tổng tiền).
- [x] Đặt hàng COD (nhập tên, số điện thoại, địa chỉ giao hàng, ghi chú).
- [x] Tra cứu & theo dõi trạng thái đơn hàng theo mã đơn.

### Quản trị (Admin)
- [x] Xem danh sách toàn bộ đơn hàng mới đặt.
- [x] Cập nhật trạng thái đơn (Chờ xác nhận, Đã xác nhận, Đang giao, Hoàn thành, Hủy).

---

## ⚡ Hướng dẫn Khởi chạy

### 1. Backend
Tạo database MySQL `highlands_db` (hoặc cấu hình thông số trong `.env` / `application.yml`).

```bash
cd backend
mvn spring-boot:run
```

### 2. Frontend
Mở trực tiếp file `frontend/index.html` trên trình duyệt hoặc qua Live Server.
Trang quản trị: `frontend/admin.html`.

---

*Xem chi tiết kế hoạch các bước và tiến độ công việc tại [docs/roadmap.md](docs/roadmap.md).*
