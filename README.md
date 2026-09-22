# Highlands Coffee Order Clone (BTL_TTCSN)

Dự án thực tập cơ sở ngành: Xây dựng website đặt hàng trực tuyến phỏng theo [Highlands Coffee Order](https://order.highlandscoffee.com.vn/).
Hệ thống tập trung vào các chức năng cốt lõi cho sinh viên, không triển khai thanh toán trực tuyến phức tạp.

---

## 📌 Tổng quan Kiến trúc & Công nghệ

- **Website mục tiêu**: [https://order.highlandscoffee.com.vn/](https://order.highlandscoffee.com.vn/)
- **Backend (`backend/`)**:
  - Java 17, Spring Boot 3.3.0.
  - Spring Data JPA, MySQL (`highlands_db`).
  - RESTful APIs có cấu hình CORS, Springdoc Swagger UI tại `/swagger-ui.html`.
  - Tự động nạp dữ liệu mẫu ban đầu qua `DataSeeder.java`.
- **Frontend (`frontend/`)**:
  - HTML5, CSS3 thuần (tông màu thương hiệu đỏ `#b22830` & kem), Vanilla JavaScript.
  - Quản lý trạng thái giỏ hàng qua `localStorage`.
  - Hoạt động độc lập, gọi API trực tiếp tới Backend.

---

## 🗂️ Cấu trúc Thư mục

```text
.
├── backend/                  # Source code Spring Boot REST API
│   ├── src/main/java/com/highlands/order/
│   │   ├── config/           # Cấu hình WebMvc CORS
│   │   ├── controller/       # REST Controllers (Category, Product, Topping, Order)
│   │   ├── dto/              # Request / Response DTOs & ApiResponse wrapper
│   │   ├── exception/        # Global Exception Handler
│   │   ├── model/            # JPA Entities (Category, Product, ProductSize, Topping, Order, OrderItem)
│   │   ├── repository/       # Spring Data JPA Repositories
│   │   ├── seeder/           # DataSeeder nạp sẵn menu mẫu
│   │   └── service/          # Business logic services
│   └── pom.xml
│
├── frontend/                 # Giao diện người dùng (HTML/CSS/JS thuần)
│   ├── css/                  # style.css, modal.css
│   ├── js/                   # api.js, cart.js, main.js, checkout.js, admin.js
│   ├── index.html            # Trang chủ, xem menu & giỏ hàng drawer
│   ├── checkout.html         # Trang điền thông tin đặt hàng (COD)
│   ├── order-status.html     # Trang tra cứu & hiển thị trạng thái đơn
│   └── admin.html            # Trang quản lý & cập nhật trạng thái đơn (Admin)
└── README.md
```

---

## 🚦 Trạng thái Hiện tại (Đã hoàn thành)

- [x] Khởi tạo khung dự án Backend Spring Boot và Frontend HTML/CSS/JS thuần.
- [x] Thiết kế mô hình dữ liệu (Category, Product, ProductSize, Topping, Order, OrderItem).
- [x] Triển khai toàn bộ REST APIs Backend (CRUD menu, tạo đơn hàng, cập nhật trạng thái đơn).
- [x] Triển khai giao diện Storefront: Menu, Modal chọn Size/Topping, Giỏ hàng `localStorage`, Form Checkout, Order Tracking.
- [x] Triển khai trang Admin quản lý đơn hàng.

---

## 🗺️ Kế hoạch Các Bước Tiếp Theo

1. **Kiểm thử tích hợp & Tinh chỉnh UI**:
   - Kiểm tra hiển thị responsive trên màn hình mobile.
   - Thêm bộ lọc tìm kiếm sản phẩm theo từ khóa trên Frontend.
2. **Nâng cao tính năng Admin (Tùy chọn)**:
   - Thêm form tạo mới / sửa sản phẩm trực tiếp từ trang Admin.
   - Thống kê doanh thu cơ bản theo ngày/tháng.
3. **Báo cáo & Đóng gói**:
   - Chuẩn bị tài liệu báo cáo môn học TTCSN.
   - Viết kịch bản demo sản phẩm.

---

## ⚡ Hướng dẫn Khởi chạy

### 1. Khởi động Backend
Yêu cầu MySQL đang chạy tại `localhost:3306`, user: `root`, password trống (cấu hình trong `backend/src/main/resources/application.yml`).

```bash
cd backend
mvn spring-boot:run
```
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 2. Khởi động Frontend
Mở trực tiếp file `frontend/index.html` trên trình duyệt hoặc dùng Live Server (VS Code).
Mở `frontend/admin.html` để truy cập trang quản trị đơn hàng.
