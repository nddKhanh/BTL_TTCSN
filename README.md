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
HỆ THỐNG ĐẶT MÓN CÀ PHÊ TRỰC TUYẾN (COFFEE ORDER SYSTEM)Báo cáo & Tài liệu Kỹ thuật Bài Tập Lớn - Thực Tập Cơ Sở Ngành (BTL_TTCSN)MỤC LỤCGiới Thiệu ChungTính Năng ChínhKiến Trúc & Công Nghệ Sử DụngCấu Trúc Cây Thư MụcYêu Cầu Môi TrườngCấu Hình & Cài Đặt Cơ Sở Dữ LiệuHướng Dẫn Khởi Chạy Backend (Spring Boot)Hướng Dẫn Khởi Chạy FrontendĐặc Tả Chi Tiết API (RESTful Endpoints)Quy Trình Git & Phối Hợp Làm Việc NhómXử Lý Các Lỗi Phổ Biến (Troubleshooting)Định Hướng Phát Triển Tiếp Theo1. GIỚI THIỆU CHUNGHệ thống Đặt Món Cà Phê Trực Tuyến là ứng dụng web cho phép người dùng đặt mua đồ uống, tùy biến thức uống (size, đá, đường, topping), theo dõi trạng thái đơn hàng theo thời gian thực và quản lý đơn hàng cá nhân. Hệ thống đồng thời cung cấp giao diện quản trị (Admin) dành cho chủ quán/nhân viên để tiếp nhận đơn hàng, cập nhật menu sản phẩm và theo dõi doanh thu.Dự án được xây dựng theo mô hình Client - Server tách biệt (Decoupled Architecture), giao tiếp thông qua giao thức chuẩn HTTP/HTTPS và chuẩn trao đổi dữ liệu RESTful JSON.2. TÍNH NĂNG CHÍNH2.1. Phân Hệ Khách Hàng (Customer)Duyệt Thực Đơn: Xem danh mục sản phẩm (Cà phê, Trà, Sinh tố, Bánh ngọt) kèm hình ảnh, mô tả và giá tiền.Tùy Chỉnh Thức Uống: Chọn mức đường, mức đá, kích cỡ (S/M/L) và các loại topping đi kèm.Giỏ Hàng (Shopping Cart): Thêm, xóa, cập nhật số lượng món; tính tự động tổng tiền và phí giao dịch.Đặt Hàng & Thanh Toán: Nhập thông tin người nhận, địa chỉ giao hàng, phương thức thanh toán (COD hoặc chuyển khoản giả lập).Quản Lý Đơn Hàng: Xem lịch sử mua hàng, chi tiết các món trong đơn hàng và tra cứu trạng thái đơn hàng (Đang chuẩn bị, Đang giao, Đã hoàn tất, Đã hủy).Xác Thực Người Dùng: Đăng ký tài khoản, đăng nhập, phân quyền và lưu trữ token xác thực.2.2. Phân Hệ Quản Trị Viên (Admin)Dashboard Thống Kê: Tổng quan số đơn trong ngày, doanh thu thực tế, số lượng người dùng.Quản Lý Đơn Hàng: Xem danh sách toàn bộ đơn hàng của khách, thay đổi trạng thái đơn (Pending -> Processing -> Completed/Cancelled).Quản Lý Menu (CRUD Sản Phẩm): Thêm món mới, cập nhật giá bán, sửa thông tin mô tả, bật/tắt tình trạng còn hàng hoặc hết hàng.Quản Lý Người Dùng: Tra cứu danh sách khách hàng và quản lý tài khoản nhân viên.3. KIẾN TRÚC & CÔNG NGHỆ SỬ DỤNG3.1. FrontendNgôn ngữ nền tảng: HTML5, CSS3 thuần (Pure CSS / Flexbox & CSS Grid).Xử lý tương tác: JavaScript hiện đại (ES6+), Fetch API, DOM Manipulation.Bộ icon và giao diện: FontAwesome 6, Google Fonts (Roboto, Montserrat).3.2. BackendNền tảng: Java 17+ (LTS).Framework: Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security).Công cụ đóng gói & quản lý build: Apache Maven Wrapper (mvnw, pom.xml).Cơ chế xác thực: JSON Web Token (JWT) / Session Token.3.3. Cơ Sở Dữ Liệu (Database)Hệ quản trị: MySQL 8.0 / MariaDB.ORM: Hibernate / Spring Data JPA.Trình quản lý trực quan: phpMyAdmin, DBeaver hoặc MySQL Workbench.4. CẤU TRÚC CÂY THƯ MỤCBTL_TTCSN-main/
│
├── backend/
│   ├── .mvn/
│   │   └── wrapper/
│   │       ├── maven-wrapper.jar
│   │       └── maven-wrapper.properties
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── coffee/
│   │   │   │           └── app/
│   │   │   │               ├── config/
│   │   │   │               │   ├── CorsConfig.java
│   │   │   │               │   └── SecurityConfig.java
│   │   │   │               ├── controller/
│   │   │   │               │   ├── AuthController.java
│   │   │   │               │   ├── OrderController.java
│   │   │   │               │   ├── ProductController.java
│   │   │   │               │   └── UserController.java
│   │   │   │               ├── dto/
│   │   │   │               │   ├── AuthRequest.java
│   │   │   │               │   ├── AuthResponse.java
│   │   │   │               │   └── OrderRequest.java
│   │   │   │               ├── model/
│   │   │   │               │   ├── Category.java
│   │   │   │               │   ├── Order.java
│   │   │   │               │   ├── OrderDetail.java
│   │   │   │               │   ├── Product.java
│   │   │   │               │   └── User.java
│   │   │   │               ├── repository/
│   │   │   │               │   ├── CategoryRepository.java
│   │   │   │               │   ├── OrderRepository.java
│   │   │   │               │   ├── ProductRepository.java
│   │   │   │               │   └── UserRepository.java
│   │   │   │               ├── service/
│   │   │   │               │   ├── AuthService.java
│   │   │   │               │   ├── OrderService.java
│   │   │   │               │   └── ProductService.java
│   │   │   │               └── CoffeeApplication.java
│   │   │   └── resources/
│   │   │       ├── static/
│   │   │       ├── templates/
│   │   │       └── application.properties
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── frontend/
│   ├── css/
│   │   ├── style.css
│   │   ├── auth.css
│   │   └── admin.css
│   ├── js/
│   │   ├── api.js
│   │   ├── auth.js
│   │   ├── cart.js
│   │   ├── orders.js
│   │   └── admin.js
│   ├── admin.html
│   ├── checkout.html
│   ├── index.html
│   ├── login.html
│   ├── my-orders.html
│   ├── order-status.html
│   ├── register.html
│   └── test.html
│
├── docs/
│   └── database_diagram.png
├── .env.example
├── .gitignore
├── PLAN_CLONE_COFFEE_ORDER.md
└── README.md
5. YÊU CẦU MÔI TRƯỜNGTrước khi chạy dự án, bạn cần đảm bảo máy tính đã cài đặt các công cụ sau:Java Development Kit (JDK 17 trở lên): Kiểm tra bằng java -version.MySQL Server (phiên bản 8.0 trở lên): Có thể chạy thông qua XAMPP, Laragon hoặc MySQL Community Server.Node.js (tùy chọn - phục vụ công cụ chạy server tĩnh frontend): Kiểm tra bằng node -v.Git: Kiểm tra bằng git --version.Visual Studio Code: Cài thêm các tiện ích khuyến nghị:Extension Pack for Java (Microsoft)Spring Boot Extension Pack (VMware)Live Server (Ritwick Dey)6. CẤU HÌNH & CÀI ĐẶT CƠ SỞ DỮ LIỆU6.1. Tạo Cơ Sở Dữ Liệu MySQLMở MySQL Workbench hoặc phpMyAdmin (thông qua XAMPP) và chạy script SQL sau:CREATE DATABASE IF NOT EXISTS `coffee_order_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `coffee_order_db`;

-- Bảng Người Dùng
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `phone` VARCHAR(20),
    `role` ENUM('ROLE_USER', 'ROLE_ADMIN') DEFAULT 'ROLE_USER',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Danh Mục
CREATE TABLE IF NOT EXISTS `categories` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT
);

-- Bảng Sản Phẩm
CREATE TABLE IF NOT EXISTS `products` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_id` INT,
    `name` VARCHAR(150) NOT NULL,
    `price` DECIMAL(12, 2) NOT NULL,
    `image_url` VARCHAR(255),
    `description` TEXT,
    `is_available` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
);

-- Bảng Đơn Hàng
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `total_amount` DECIMAL(12, 2) NOT NULL,
    `status` ENUM('PENDING', 'PROCESSING', 'DELIVERING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    `shipping_address` VARCHAR(255) NOT NULL,
    `phone_contact` VARCHAR(20) NOT NULL,
    `payment_method` VARCHAR(50) DEFAULT 'COD',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
);

-- Bảng Chi Tiết Đơn Hàng
CREATE TABLE IF NOT EXISTS `order_details` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `size` VARCHAR(10) DEFAULT 'M',
    `unit_price` DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE
);

-- Dữ liệu danh mục mẫu
INSERT INTO `categories` (`name`, `description`) VALUES
('Cà phê', 'Các dòng cà phê pha máy và pha phin truyền thống'),
('Trà trái cây', 'Trà thanh nhiệt và hoa quả tươi mát'),
('Bánh ngọt', 'Bánh ăn kèm đồ uống');

-- Dữ liệu sản phẩm mẫu
INSERT INTO `products` (`category_id`, `name`, `price`, `image_url`, `description`) VALUES
(1, 'Cà Phê Sữa Đá', 29000, 'https://images.unsplash.com/photo-1517256064527-09c73fc73e38', 'Đậm đà hương vị truyền thống Việt Nam'),
(1, 'Bạc Xỉu', 32000, 'https://images.unsplash.com/photo-1541167760496-1628856ab772', 'Vị sữa ngọt béo hòa cùng chút đăng đắng cà phê'),
(1, 'Americano', 35000, 'https://images.unsplash.com/photo-1509785307050-d4066910ec1e', 'Espresso hòa quyện cùng nước nóng thanh nhẹ'),
(2, 'Trà Đào Cam Sả', 39000, 'https://images.unsplash.com/photo-1556679343-c7306c1976bc', 'Thơm nồng hương sả kết hợp đào giòn ngọt ngào'),
(3, 'Tiramisu', 42000, 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9', 'Bánh mềm xốp vị cà phê và phô mai mascarpone');
7. HƯỚNG DẪN KHỞI CHẠY BACKEND (SPRING BOOT)7.1. Cấu Hình Biến Kết NốiMở file backend/src/main/resources/application.properties và đối chiếu thông tin kết nối Database của máy bạn:# Cấu hình cổng chạy máy chủ
server.port=8080

# Cấu hình kết nối cơ sở dữ liệu
spring.datasource.url=jdbc:mysql://localhost:3306/coffee_order_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Cấu hình Hibernate JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Thời gian sống JWT (nếu có sử dụng)
jwt.secret=9a4f2c8d7e1b5a3f0c2d8e6a1b4c7d9e
jwt.expiration=86400000
7.2. Lệnh Khởi Chạy Trong TerminalMở Terminal của VS Code (Ctrl + ~) và chạy các lệnh sau:Di chuyển vào thư mục backend:cd backend
(Nếu thư mục dự án bị lồng 2 cấp BTL_TTCSN-main, gõ: cd BTL_TTCSN-main/backend)Khởi động server bằng Maven Wrapper:.\mvnw.cmd spring-boot:run
(Trên Linux/macOS dùng lệnh: ./mvnw spring-boot:run)Nếu gặp thông báo lỗi phân quyền PowerShell (Restricted Script Execution), sử dụng:cmd /c mvnw spring-boot:run
Kiểm tra: Khi thấy dòng chữ tương tự dưới đây xuất hiện trên console, server đã sẵn sàng tiếp nhận request:Tomcat started on port 8080 (http) with context path '/'
Started CoffeeApplication in 3.421 seconds
8. HƯỚNG DẪN KHỞI CHẠY FRONTENDDo Frontend được xây dựng bằng HTML, CSS và JavaScript thuần, bạn có thể áp dụng 1 trong 3 cách sau:Cách 1: Sử Dụng Tiện Ích Live Server (Được Khuyên Dùng)Cài đặt tiện ích Live Server từ chợ tiện ích mở rộng của VS Code.Tại cây thư mục bên trái, click chuột phải vào file frontend/index.html.Nhấp chọn Open with Live Server.Trình duyệt mặc định sẽ mở trang tại địa chỉ: http://127.0.0.1:5500/frontend/index.html.Cách 2: Mở Trực Tiếp Trên Trình DuyệtClick chuột phải vào file frontend/index.html > chọn Reveal in File Explorer.Nhấp đúp chuột vào file để mở trực tiếp trong trình duyệt Chrome/Edge/Firefox.Lưu ý: Cách này có thể phát sinh lỗi CORS nếu mã nguồn gọi fetch trực tiếp sang http://localhost:8080.Cách 3: Chạy Thông Qua Node.jsNếu máy tính đã có cài sẵn Node.js:cd frontend
npx serve
Mở đường dẫn xuất hiện trên console (mặc định là http://localhost:3000).9. ĐẶC TẢ CHI TIẾT API (RESTFUL ENDPOINTS)Tất cả các API được cung cấp với tiền tố (Base URL): http://localhost:8080/api9.1. Xác Thực (Authentication)Phương ThứcEndpointMô TảYêu Cầu TokenPOST/api/auth/registerĐăng ký tài khoản người dùng mớiKhôngPOST/api/auth/loginĐăng nhập tài khoản, trả về thông tin và TokenKhôngGET/api/auth/meLấy thông tin tài khoản đang đăng nhậpCóMẫu Request Đăng Nhập (POST /api/auth/login):{
  "username": "nguyenvana",
  "password": "Password@123"
}
Mẫu Response:{
  "status": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "userId": 1,
    "username": "nguyenvana",
    "role": "ROLE_USER"
  }
}
9.2. Quản Lý Sản Phẩm (Products)Phương ThứcEndpointMô TảYêu Cầu Phân QuyềnGET/api/productsLấy danh sách toàn bộ sản phẩmCông khaiGET/api/products/{id}Lấy thông tin chi tiết một sản phẩmCông khaiPOST/api/productsThêm sản phẩm mới vào thực đơnQuản trị viên (Admin)PUT/api/products/{id}Chỉnh sửa thông tin sản phẩmQuản trị viên (Admin)DELETE/api/products/{id}Xóa sản phẩm khỏi hệ thốngQuản trị viên (Admin)9.3. Quản Lý Đơn Hàng (Orders)Phương ThứcEndpointMô TảYêu Cầu Phân QuyềnPOST/api/ordersTạo đơn hàng mới từ giỏ hàngKhách hàng đã đăng nhậpGET/api/orders/my-ordersXem danh sách đơn hàng của người dùng hiện tạiKhách hàng đã đăng nhậpGET/api/orders/{id}Lấy chi tiết đơn hàng theo mã đơnKhách hàng / AdminGET/api/admin/ordersLấy tất cả đơn hàng hệ thốngQuản trị viên (Admin)PUT/api/admin/orders/{id}/statusCập nhật trạng thái đơn hàngQuản trị viên (Admin)Mẫu Request Tạo Đơn Hàng (POST /api/orders):{
  "shippingAddress": "123 Đường Cầu Giấy, Hà Nội",
  "phoneContact": "0987654321",
  "paymentMethod": "COD",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "size": "L",
      "unitPrice": 35000
    },
    {
      "productId": 4,
      "quantity": 1,
      "size": "M",
      "unitPrice": 39000
    }
  ]
}
10. QUY TRÌNH GIT & PHỐI HỢP LÀM VIỆC NHÓM10.1. Nguyên Tắc Làm ViệcKhông commit và push trực tiếp code chưa được kiểm thử lên nhánh chính main hoặc master.Luôn kéo mã nguồn mới nhất về trước khi bắt đầu viết code mới (git pull).Đặt tên commit ngắn gọn, rõ ràng theo chuẩn Conventional Commits (ví dụ: feat:, fix:, docs:, refactor:).10.2. Các Lệnh Thường DùngKiểm tra trạng thái các file đã sửa đổi:git status
Thêm thay đổi vào Staging:git add .
Tạo bản commit với thông điệp rõ ràng:git commit -m "feat: hoàn thiện giao diện trang giỏ hàng"
Cập nhật mã nguồn mới nhất từ remote repository:git pull origin main
Đẩy commit lên nhánh làm việc:git push origin main
11. XỬ LÝ CÁC LỖI PHỔ BIẾN (TROUBLESHOOTING)11.1. Lỗi: npm error code ENOENT ... package.jsonNguyên nhân: Thư mục backend sử dụng Java Spring Boot/Maven, không dùng Node.js, nên không có file package.json.Cách xử lý: Không chạy lệnh npm install trong thư mục backend. Hãy dùng lệnh .\mvnw.cmd spring-boot:run.11.2. Lỗi: Communications link failure / Connection refused: connectNguyên nhân: Máy tính chưa bật dịch vụ MySQL Server hoặc cấu hình sai thông số cổng/tài khoản trong file application.properties.Cách xử lý:Mở XAMPP Control Panel và nhấn Start tại ô MySQL.Kiểm tra tài khoản và mật khẩu MySQL trong file backend/src/main/resources/application.properties.Đảm bảo database có tên coffee_order_db đã được tạo.11.3. Lỗi: Access to fetch at ... from origin ... has been blocked by CORS policyNguyên nhân: Backend chặn request gửi từ tên miền hoặc cổng khác của frontend (ví dụ frontend chạy tại cổng 5500, backend chạy cổng 8080).Cách xử lý: Đảm bảo Backend đã được cấu hình thêm header @CrossOrigin(origins = "*") trên Controller hoặc đã cấu hình lớp CorsConfiguration cho phép origin http://127.0.0.1:5500.11.4. Lỗi: Restricted Mode trên giao diện VS CodeNguyên nhân: VS Code kích hoạt chế độ bảo vệ khi mở thư mục tải từ bên ngoài về máy.Cách xử lý: Nhấp vào dòng chữ Manage ở banner màu xanh trên cùng của VS Code > chọn Trust folder & all features.12. ĐỊNH HƯỚNG PHÁT TRIỂN TIẾP THEO[ ] Tích hợp cổng thanh toán trực tuyến chính thức (VNPay, MoMo QR hoặc ZaloPay Sandbox).[ ] Xây dựng thông báo trạng thái đơn hàng thời gian thực qua WebSocket thay vì gọi polling API.[ ] Bổ sung chức năng đánh giá, bình luận và xếp hạng sao cho từng món uống sau khi hoàn thành đơn.[ ] Nâng cấp giao diện sang các thư viện hiện đại (Vue.js / React) để tối ưu hóa trải nghiệm người dùng đơn trang (SPA).