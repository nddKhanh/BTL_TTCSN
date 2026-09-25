# Kế hoạch dự án — Website đặt hàng cà phê

> **Mục đích:** Xây dựng một website đặt hàng cà phê cho dự án sinh viên, lấy cảm hứng từ luồng trải nghiệm của Highlands Coffee Order nhưng sử dụng **tên, logo, hình ảnh và nội dung riêng**.
>
> **Nguyên tắc phạm vi:** Ưu tiên luồng CRUD hoàn chỉnh, dễ demo và dễ bảo vệ; không phát triển các nghiệp vụ thương mại điện tử phức tạp.

## 1. Bối cảnh và mục tiêu

Trang tham khảo có các thành phần chính: danh mục đồ uống/sản phẩm, chương trình khuyến mãi, danh sách sản phẩm, tìm kiếm, tài khoản và giỏ hàng. Dự án sẽ mô phỏng các luồng cốt lõi này ở mức phù hợp với thời gian làm bài sinh viên.

### Mục tiêu chức năng

- Khách xem, tìm kiếm và chọn sản phẩm.
- Khách quản lý giỏ hàng, áp mã giảm giá và tạo đơn đặt hàng.
- Khách đăng ký/đăng nhập, xem các đơn của mình.
- Quản trị viên quản lý danh mục, sản phẩm, mã giảm giá, đơn hàng và người dùng cơ bản.
- Hệ thống có phân quyền `CUSTOMER` và `ADMIN`.

### Ngoài phạm vi (không cần làm)

- Kết nối cổng thanh toán thật (VNPAY/MoMo).
- Định vị chi nhánh, giao hàng thời gian thực hoặc tối ưu tuyến giao hàng.
- Tích điểm thành viên, voucher nhiều điều kiện phức tạp.
- Flash sale có đồng hồ đếm ngược, tồn kho đa kho, đánh giá sản phẩm, chatbot.
- Sao chép nhận diện thương hiệu hay dữ liệu của Highlands Coffee.

## 2. Vai trò người dùng

| Vai trò | Quyền chính |
| --- | --- |
| Khách vãng lai | Xem danh mục/sản phẩm, tìm kiếm, thêm giỏ hàng; có thể đặt hàng mà không cần tài khoản nếu nhóm chọn hỗ trợ guest checkout. |
| Khách hàng (`CUSTOMER`) | Các quyền khách vãng lai, quản lý thông tin cá nhân và xem lịch sử đơn hàng. |
| Quản trị viên (`ADMIN`) | CRUD danh mục, sản phẩm, mã giảm giá; xem và cập nhật trạng thái đơn; xem danh sách người dùng. |

## 3. Phạm vi giao diện và chức năng

### 3.1. Khu vực khách hàng

#### Trang chủ

- Header: logo dự án, menu danh mục, ô/nút tìm kiếm, tài khoản, biểu tượng giỏ hàng.
- Banner giới thiệu thương hiệu hoặc chương trình đang diễn ra.
- Các danh mục nổi bật, ví dụ: Cà phê, Trà, Freeze, Bánh ngọt, Đồ uống khác.
- Khu vực sản phẩm bán chạy/sản phẩm mới.
- Khu vực mã giảm giá: hiển thị mã và điều kiện áp dụng.
- Footer: thông tin liên hệ giả lập, chính sách, form đăng ký nhận tin (chỉ lưu email nếu triển khai).

#### Danh sách sản phẩm

- Lọc theo danh mục.
- Tìm theo tên sản phẩm.
- Sắp xếp tùy chọn: giá tăng dần/giảm dần, mới nhất (nếu còn thời gian).
- Phân trang hoặc nút “Xem thêm”.
- Thẻ sản phẩm gồm ảnh, tên, giá, giá khuyến mãi (nếu có) và nút thêm giỏ.

#### Chi tiết sản phẩm

- Hiển thị ảnh, tên, mô tả, giá, tồn kho (nếu áp dụng).
- Chọn số lượng.
- Tuỳ chọn đơn giản cho đồ uống: size S/M/L, nóng/đá; có thể cộng phụ thu.
- Ô ghi chú cho món.
- Nút thêm vào giỏ hàng.

#### Giỏ hàng

- Hiển thị từng dòng sản phẩm, lựa chọn, ghi chú, đơn giá, số lượng và thành tiền.
- Tăng/giảm số lượng và xóa sản phẩm.
- Nhập mã giảm giá.
- Hiển thị tạm tính, tiền giảm, phí giao hàng giả lập và tổng tiền.
- Giữ giỏ qua `localStorage` với khách vãng lai; đồng bộ với backend sau đăng nhập là phần nâng cao, không bắt buộc.

#### Đặt hàng

- Thu thập: họ tên, số điện thoại, địa chỉ nhận hàng, ghi chú chung.
- Chọn phương thức: COD hoặc “Thanh toán demo”.
- Tạo đơn trạng thái ban đầu `PENDING`.
- Trang/cửa sổ xác nhận đặt hàng thành công kèm mã đơn.

#### Tài khoản

- Đăng ký, đăng nhập, đăng xuất.
- Cập nhật tên và số điện thoại.
- Xem danh sách đơn đã tạo, chi tiết đơn và trạng thái hiện tại.

### 3.2. Khu vực quản trị

#### Dashboard (tối giản)

- Tổng số đơn hàng.
- Tổng doanh thu từ đơn `COMPLETED` hoặc mọi đơn không bị hủy (nhóm cần chọn một quy ước và ghi rõ).
- Số sản phẩm, số người dùng.
- Danh sách vài đơn mới nhất.

#### Quản lý danh mục

- Tạo, xem, sửa, ẩn/xóa danh mục.
- Không cho xóa danh mục nếu còn sản phẩm; thay bằng ẩn danh mục hoặc yêu cầu chuyển sản phẩm sang danh mục khác.

#### Quản lý sản phẩm

- Tạo, xem, sửa, ẩn/xóa sản phẩm.
- Thuộc tính: tên, danh mục, giá, giá giảm, mô tả, ảnh, tồn kho, trạng thái hiển thị.
- Kiểm tra dữ liệu: giá không âm; giá giảm không lớn hơn giá gốc; tên không rỗng.

#### Quản lý mã giảm giá

- CRUD mã giảm giá.
- Hai loại: phần trăm (`PERCENT`) và số tiền cố định (`FIXED`).
- Điều kiện tối thiểu: giá trị đơn hàng tối thiểu, ngày bắt đầu/kết thúc, trạng thái hoạt động.
- Một đơn chỉ dùng một mã.

#### Quản lý đơn hàng

- Xem danh sách, lọc theo trạng thái, mở chi tiết.
- Cập nhật trạng thái theo luồng:

```text
PENDING → CONFIRMED → DELIVERING → COMPLETED
PENDING/CONFIRMED → CANCELLED
```

- Không cho sửa lại đơn đã `COMPLETED` hoặc `CANCELLED`.

## 4. Luồng nghiệp vụ quan trọng

### 4.1. Đặt hàng

```text
Xem sản phẩm
  → chọn tuỳ chọn / số lượng
  → thêm giỏ hàng
  → chỉnh sửa giỏ
  → áp mã giảm giá (tuỳ chọn)
  → nhập thông tin nhận hàng
  → tạo đơn PENDING
  → hiển thị xác nhận đơn
```

### 4.2. Tính giá

```text
tạm tính = tổng (đơn giá sản phẩm + phụ thu tuỳ chọn) × số lượng
giảm giá = giá trị coupon hợp lệ, không vượt quá tạm tính
tổng tiền = tạm tính - giảm giá + phí giao hàng
```

Quy ước demo khuyến nghị: `shippingFee = 15.000 VNĐ`; miễn phí giao hàng nếu tạm tính từ `200.000 VNĐ` trở lên. Có thể để các giá trị này trong biến cấu hình backend.

### 4.3. Mã giảm giá

Mã hợp lệ khi:

- Có tồn tại và đang `is_active = true`.
- Chưa hết hạn và đã đến ngày bắt đầu.
- Giá trị tạm tính đạt `min_order_value`.
- Nếu loại `PERCENT`, giảm = `subtotal × discount_value / 100`.
- Nếu loại `FIXED`, giảm = `discount_value`.

## 5. Thiết kế dữ liệu

### 5.1. Bảng chính

| Bảng | Trường quan trọng | Ghi chú |
| --- | --- | --- |
| `users` | `id`, `full_name`, `email`, `password_hash`, `phone`, `role`, `created_at` | `email` duy nhất; `role`: `CUSTOMER`, `ADMIN`. |
| `categories` | `id`, `name`, `slug`, `image_url`, `is_active` | `slug` duy nhất, dùng cho URL. |
| `products` | `id`, `category_id`, `name`, `slug`, `description`, `price`, `sale_price`, `image_url`, `stock`, `is_active`, `created_at` | Một sản phẩm thuộc một danh mục. |
| `product_options` | `id`, `product_id`, `option_name`, `option_value`, `extra_price` | Không bắt buộc ở phiên bản đầu; dùng cho size/nóng-đá. |
| `carts` | `id`, `user_id`, `session_id` | Một trong hai trường nhận diện giỏ hàng có giá trị. |
| `cart_items` | `id`, `cart_id`, `product_id`, `quantity`, `unit_price`, `note` | Có thể lưu `option_summary` dạng text/JSON đơn giản. |
| `coupons` | `id`, `code`, `discount_type`, `discount_value`, `min_order_value`, `start_at`, `end_at`, `is_active` | `code` duy nhất. |
| `orders` | `id`, `user_id`, `receiver_name`, `phone`, `address`, `subtotal`, `discount_amount`, `shipping_fee`, `total_amount`, `coupon_code`, `payment_method`, `status`, `created_at` | Lưu snapshot tổng tiền lúc đặt đơn. |
| `order_items` | `id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `option_summary`, `note` | Bắt buộc lưu snapshot tên/giá để đơn cũ không thay đổi theo sản phẩm. |

### 5.2. Quan hệ

```text
Category 1 ─── n Product
Product  1 ─── n ProductOption
User     1 ─── n Order
Order    1 ─── n OrderItem
Cart     1 ─── n CartItem
Product  1 ─── n CartItem / OrderItem
User     0..1 ─ n Cart
```

## 6. API REST dự kiến

Tiền tố chung: `/api`.

### Xác thực và công khai

```http
POST /auth/register
POST /auth/login
GET  /categories
GET  /products?keyword=&category=&page=&size=&sort=
GET  /products/{slug}
POST /coupons/validate
```

### Giỏ hàng và đơn hàng

```http
GET    /cart
POST   /cart/items
PUT    /cart/items/{id}
DELETE /cart/items/{id}

POST /orders
GET  /orders/my-orders
GET  /orders/{id}
```

### Người dùng đã đăng nhập

```http
GET /users/me
PUT /users/me
```

### Quản trị (`ADMIN`)

```http
GET/POST/PUT/DELETE /admin/categories
GET/POST/PUT/DELETE /admin/products
GET/POST/PUT/DELETE /admin/coupons
GET                 /admin/orders
GET                 /admin/orders/{id}
PUT                 /admin/orders/{id}/status
GET                 /admin/users
GET                 /admin/dashboard
```

## 7. Kiến trúc và công nghệ đề xuất

### Phương án chính: Java full-stack tách frontend/backend

| Tầng | Công nghệ |
| --- | --- |
| Frontend | React, Vite, React Router, Axios, Tailwind CSS hoặc Material UI |
| Backend | Java 21 (hoặc phiên bản theo môn học), Spring Boot 3, Spring Web, Spring Data JPA, Spring Security |
| Xác thực | JWT, BCrypt để băm mật khẩu |
| Cơ sở dữ liệu | MySQL 8 |
| Lưu ảnh | Cloudinary (khuyến nghị) hoặc thư mục static local khi demo |
| Kiểm thử API | Postman/Bruno; JUnit cho các service quan trọng |
| Triển khai | Vercel/Netlify cho frontend; Render/Railway cho backend và MySQL, hoặc chạy Docker/local để demo |

### Cấu trúc backend gợi ý

```text
src/main/java/.../
├── config/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
├── security/
├── exception/
└── mapper/
```

Không trả trực tiếp Entity ra API; dùng DTO request/response. Các endpoint quản trị phải kiểm tra role ở tầng Spring Security.

### Cấu trúc frontend gợi ý

```text
src/
├── api/
├── components/
├── pages/
│   ├── customer/
│   └── admin/
├── layouts/
├── hooks/
├── context/ hoặc store/
├── routes/
└── utils/
```

## 8. Kế hoạch thực hiện 4 tuần

| Tuần | Hạng mục | Đầu ra cần có |
| --- | --- | --- |
| 1 | Chốt yêu cầu, wireframe/Figma, ERD, khởi tạo repo; tạo database; xác thực và phân quyền | Có thể đăng ký/đăng nhập; schema và API cơ sở hoạt động. |
| 2 | CRUD danh mục/sản phẩm phía admin; trang chủ, danh sách, tìm kiếm, chi tiết sản phẩm | Admin thêm sản phẩm và khách nhìn thấy ngay. |
| 3 | Giỏ hàng, tùy chọn sản phẩm, coupon, checkout, tạo đơn, lịch sử đơn | Hoàn thành luồng đặt hàng đầu-cuối. |
| 4 | Quản lý trạng thái đơn, dashboard, responsive, seed data, test, deploy, báo cáo và kịch bản demo | Bản demo ổn định, có dữ liệu minh họa và tài liệu. |

## 9. Thứ tự ưu tiên phát triển

### MVP bắt buộc

1. Xác thực và phân quyền.
2. CRUD danh mục/sản phẩm.
3. Hiển thị, tìm kiếm và lọc sản phẩm.
4. Giỏ hàng.
5. Tạo đơn và quản lý trạng thái đơn.
6. Giao diện responsive cơ bản.

### Làm sau khi MVP đã ổn định

1. Coupon.
2. Tùy chọn size/nóng-đá.
3. Dashboard số liệu.
4. Upload ảnh Cloudinary.
5. Sắp xếp sản phẩm, phân trang, newsletter.

## 10. Dữ liệu mẫu cần chuẩn bị

- 5–7 danh mục.
- 20–30 sản phẩm kèm ảnh hợp lệ, giá, mô tả ngắn.
- 2–3 tài khoản khách hàng và 1 tài khoản admin.
- 2–3 coupon mẫu, ví dụ `WELCOME10` giảm 10%, `FREESHIP` (nếu mô hình dữ liệu hỗ trợ).
- 8–10 đơn ở nhiều trạng thái để dashboard và danh sách đơn có ý nghĩa.

## 11. Tiêu chí nghiệm thu

- Khách đăng ký, đăng nhập, đăng xuất thành công.
- Khách xem được danh mục, tìm kiếm sản phẩm và xem chi tiết.
- Khách thêm/sửa/xóa sản phẩm trong giỏ và tổng tiền thay đổi đúng.
- Coupon chỉ được áp dụng khi hợp lệ.
- Tạo đơn thành công và có mã đơn/trang xác nhận.
- Khách xem được đơn của chính mình, không xem được đơn của người khác.
- Admin CRUD được danh mục, sản phẩm và coupon.
- Admin cập nhật được trạng thái đơn theo đúng luồng.
- Các trang chính hiển thị tốt trên mobile và desktop.
- Không để lộ mật khẩu; API admin được bảo vệ bằng xác thực/phân quyền.

## 12. Kịch bản demo đề xuất

1. Admin đăng nhập, tạo danh mục và sản phẩm mới.
2. Chuyển sang tài khoản khách; sản phẩm vừa tạo xuất hiện trên trang danh sách.
3. Khách tìm sản phẩm, chọn size/số lượng, thêm giỏ.
4. Khách áp coupon hợp lệ và tạo đơn COD.
5. Admin mở danh sách đơn, xác nhận rồi cập nhật trạng thái giao hàng/hoàn thành.
6. Khách mở lịch sử đơn để thấy trạng thái đã thay đổi.

## 13. Việc cần quyết định trước khi code

- Chốt tên thương hiệu riêng, bảng màu và asset ảnh không thuộc Highlands Coffee.
- Chọn React + Spring Boot + MySQL như kế hoạch trên, hoặc xác nhận một stack khác.
- Quyết định có cho khách đặt hàng không cần đăng nhập hay bắt buộc đăng nhập.
- Quyết định giỏ hàng được lưu `localStorage` (nhanh nhất) hay quản lý hoàn toàn bằng backend.
- Chốt quy tắc phí giao hàng và coupon để không đổi giữa chừng.

## 14. Gợi ý commit Git

```text
chore: initialize frontend and spring boot backend
feat(auth): add registration login and jwt authorization
feat(admin): implement category and product management
feat(catalog): add product listing search and detail pages
feat(cart): implement cart item management and price calculation
feat(order): create checkout and customer order history
feat(admin): add order status management and dashboard
test: add service and controller tests
docs: add setup guide and API documentation
```

---

## Ghi chú bàn giao cho phiên làm việc tiếp theo

Khi bắt đầu phát triển, kiểm tra cấu trúc repository hiện tại trước. Nếu chưa có mã nguồn, ưu tiên tạo skeleton theo kiến trúc ở mục 7, sau đó triển khai theo thứ tự MVP ở mục 9. Không bắt đầu các tính năng nâng cao trước khi hoàn thành và kiểm thử luồng: **sản phẩm → giỏ hàng → tạo đơn → admin đổi trạng thái**.
