# Checklist dự án — Website đặt hàng cà phê

> **Cách dùng:** Khi hoàn tất một việc, đổi `- [ ]` thành `- [x]`, đồng thời ghi ngày/ghi chú ngắn ngay dưới mục đó nếu cần. Phiên làm việc sau phải đọc phần **Tiến độ hiện tại** và chỉ làm các mục chưa đánh dấu.
>
> **Phạm vi:** Lấy cảm hứng từ luồng đặt hàng cà phê, nhưng dùng tên, logo, hình ảnh và nội dung riêng; không sao chép nhận diện hay dữ liệu của Highlands Coffee.

## Tiến độ hiện tại

- [x] Khởi tạo: frontend Vanilla JS và backend Spring Boot 4.1.1 có Maven Wrapper.
- [ ] MVP: chưa bắt đầu.
- [ ] Tính năng nâng cao: chưa bắt đầu.
- [ ] Kiểm thử, triển khai và tài liệu: chưa bắt đầu.
- [ ] Luồng demo hoàn chỉnh: chưa xác nhận.

**Ghi chú phiên gần nhất (2026-09-26):** Maven Wrapper build thành công. Catalog hỗ trợ lọc/tìm; đơn hàng lưu tạm tính, phí giao và tổng tiền do backend tính. Backend có JWT/BCrypt, phân quyền admin và CRUD danh mục/sản phẩm.

---

## 1. Chốt phạm vi và quyết định

- [x] Chốt thương hiệu demo `Mộc Nhiên Coffee`; thay toàn bộ nhãn/URL ảnh Highlands bằng nội dung riêng và placeholder trung lập.
- [x] Chốt stack hiện tại: Vanilla JS + Spring Boot 4.1.1 + MySQL.
- [x] Chốt Java 17 theo cấu hình Maven hiện tại.
- [x] Hỗ trợ khách vãng lai đặt hàng; tài khoản dùng cho hồ sơ và khu vực quản trị.
- [x] Lưu giỏ khách vãng lai bằng `localStorage`.
- [x] Phí ship 15.000 VNĐ; miễn phí từ 200.000 VNĐ; cấu hình qua biến môi trường.
- [ ] Chốt quy ước doanh thu dashboard: chỉ `COMPLETED` hoặc mọi đơn chưa hủy.
- [ ] Xác nhận ngoài phạm vi: thanh toán thật, định vị/giao hàng thời gian thực, tích điểm, flash sale, đa kho, review, chatbot.

## 2. Thiết kế trước khi code

- [ ] Lập wireframe/Figma cho khách hàng: trang chủ, danh sách, chi tiết, giỏ, checkout, tài khoản.
- [ ] Lập wireframe/Figma cho admin: dashboard, danh mục, sản phẩm, coupon, đơn hàng, người dùng.
- [ ] Vẽ ERD và xác nhận quan hệ dữ liệu.
- [ ] Định nghĩa enum `PERCENT`, `FIXED` cho coupon.
- [x] Định nghĩa enum quyền `CUSTOMER`, `ADMIN`.
- [x] Định nghĩa enum trạng thái đơn: `PENDING`, `CONFIRMED`, `DELIVERING`, `COMPLETED`, `CANCELLED`.
- [ ] Viết/quy ước hợp đồng API REST và DTO request/response.
- [ ] Chốt định dạng lỗi API và các quy tắc validation.

### Checklist mô hình dữ liệu

- [x] `users`: email duy nhất; lưu `full_name`, `password_hash`, `phone`, `role`, `created_at`.
- [ ] `categories`: `name`, `slug` duy nhất, `image_url`, `is_active`.
- [ ] `products`: danh mục, tên/slug, mô tả, giá, giá giảm, ảnh, tồn kho, trạng thái, ngày tạo.
- [ ] `product_options` (làm sau MVP): tên lựa chọn, giá trị, phụ thu.
- [ ] `carts` và `cart_items`: hỗ trợ `user_id` hoặc `session_id`; item lưu giá tại thời điểm thêm.
- [ ] `coupons`: mã duy nhất, loại/giá trị giảm, giá trị đơn tối thiểu, thời hạn, trạng thái.
- [ ] `orders`: lưu snapshot tổng tiền, mã coupon, phương thức thanh toán, trạng thái và thông tin nhận hàng.
- [ ] `order_items`: lưu snapshot tên/giá sản phẩm, lựa chọn và ghi chú.
- [ ] Xác nhận quan hệ: Category–Product, Product–ProductOption, User–Order, Order–OrderItem, Cart–CartItem.

## 3. Khởi tạo dự án và hạ tầng

- [x] Kiểm tra cấu trúc repository hiện tại, không ghi đè mã có sẵn.
- [ ] Chuẩn hoá frontend theo stack đã chốt (hiện dùng Vanilla JS, chưa phải React/Vite).
- [x] Khởi tạo backend Spring Boot với Web, Data JPA, Validation và Security.
- [x] Hoàn thiện các tầng `config`, `controller`, `dto`, `model`, `repository`, `service`, `security`, `exception`.
- [ ] Chuẩn hoá cấu trúc frontend theo kế hoạch.
- [x] Cấu hình database và biến môi trường cơ bản.
- [x] Thiết lập CORS và README chạy local.
- [ ] Thiết lập công cụ kiểm thử API (Postman hoặc Bruno).

## 4. MVP bắt buộc

### 4.1. Xác thực và phân quyền

- [x] Đăng ký tài khoản.
- [x] Đăng nhập và tạo JWT; secret chỉ đọc từ biến môi trường `JWT_SECRET`.
- [x] Băm mật khẩu bằng BCrypt; API chỉ trả DTO hồ sơ, không trả password.
- [ ] Đăng xuất ở frontend.
- [x] Bảo vệ endpoint yêu cầu đăng nhập.
- [x] Phân quyền `CUSTOMER` và `ADMIN`; endpoint admin đơn hàng yêu cầu role `ADMIN`.
- [x] API `GET /users/me` và `PUT /users/me` (tiền tố thực tế `/api/v1`).
- [ ] UI cập nhật tên và số điện thoại.

### 4.2. Danh mục và sản phẩm (admin)

- [x] CRUD danh mục qua API admin.
- [x] Không xóa danh mục còn sản phẩm.
- [x] CRUD sản phẩm qua API admin; thao tác xóa sẽ ẩn sản phẩm.
- [x] Validation: tên không rỗng và giá không âm (giá giảm chưa có trong mô hình hiện tại).
- [ ] Hỗ trợ ảnh local/static trước; upload Cloudinary là phần sau MVP.
- [x] API admin danh mục/sản phẩm được bảo vệ bằng role `ADMIN`.

### 4.3. Catalog khách hàng

- [ ] Trang chủ: header, banner, danh mục nổi bật, sản phẩm mới/bán chạy, mã giảm giá, footer.
- [x] Danh sách sản phẩm.
- [x] Lọc theo danh mục.
- [x] Tìm theo tên sản phẩm, kết hợp được với bộ lọc danh mục.
- [x] Hiển thị thẻ sản phẩm: ảnh, tên, giá và nút thêm giỏ.
- [x] Trang chi tiết: ảnh, tên, mô tả, giá, số lượng và thêm giỏ.
- [x] API đọc danh mục/sản phẩm (hiện dùng `/api/v1` và định danh sản phẩm bằng ID).

### 4.4. Giỏ hàng

- [x] Hiển thị sản phẩm, đơn giá, số lượng và thành tiền.
- [x] Tăng/giảm số lượng, xóa sản phẩm.
- [x] Lưu giỏ bằng `localStorage`.
- [x] Tính tạm tính trên frontend.
- [ ] API `GET /cart`, `POST /cart/items`, `PUT /cart/items/{id}`, `DELETE /cart/items/{id}` (nếu dùng backend).

### 4.5. Checkout, đơn hàng và trạng thái

- [x] Form nhận hàng: họ tên, điện thoại, địa chỉ, ghi chú chung.
- [x] Chọn COD.
- [x] Tính phí ship theo quy tắc đã chốt ở backend; lưu snapshot tạm tính/phí ship/tổng tiền.
- [x] Tạo đơn với trạng thái đầu `PENDING`.
- [x] Trang xác nhận/tra cứu đơn kèm mã đơn.
- [ ] Khách xem lịch sử đơn theo tài khoản (hiện chỉ tra cứu bằng mã đơn).
- [x] Admin xem danh sách, chi tiết đơn và cập nhật trạng thái; còn thiếu lọc.
- [x] Cho phép luồng `PENDING → CONFIRMED → DELIVERING → COMPLETED`.
- [x] Chỉ cho phép hủy từ `PENDING` hoặc `CONFIRMED`.
- [x] Chặn sửa đơn đã `COMPLETED` hoặc `CANCELLED`.
- [x] API tạo/tra cứu đơn và cập nhật trạng thái (hiện dùng `/api/v1/orders`).

### 4.6. Responsive và kiểm thử MVP

- [ ] Các trang chính dùng tốt trên mobile và desktop.
- [ ] Kiểm thử luồng bắt buộc: sản phẩm → giỏ hàng → tạo đơn → admin đổi trạng thái.
- [ ] Kiểm thử phân quyền và truy cập đơn hàng của người khác.
- [ ] Sửa các lỗi blocker trước khi chuyển sang tính năng nâng cao.

## 5. Tính năng sau MVP

- [ ] Coupon: CRUD admin.
- [ ] Coupon: API `POST /coupons/validate`.
- [ ] Coupon: kiểm tra active, thời hạn, giá trị đơn tối thiểu và chỉ một mã/đơn.
- [ ] Coupon: giảm giá không vượt tạm tính; hỗ trợ `PERCENT` và `FIXED`.
- [ ] Tuỳ chọn sản phẩm: size S/M/L, nóng/đá, phụ thu, ghi chú món.
- [ ] Lưu `option_summary` tại giỏ và `order_items`.
- [ ] Dashboard: tổng đơn, doanh thu theo quy ước đã chốt, số sản phẩm/người dùng, đơn mới nhất.
- [ ] Sắp xếp giá/mới nhất, phân trang hoặc “Xem thêm”.
- [ ] Upload ảnh Cloudinary.
- [ ] Newsletter chỉ lưu email (nếu còn thời gian).

## 6. Dữ liệu mẫu

- [ ] Tạo 5–7 danh mục.
- [ ] Tạo 20–30 sản phẩm có ảnh, giá và mô tả ngắn.
- [ ] Tạo 2–3 khách hàng và 1 admin.
- [ ] Tạo 2–3 coupon, ví dụ `WELCOME10`.
- [ ] Tạo 8–10 đơn ở nhiều trạng thái.
- [ ] Xác nhận seed data phù hợp demo và không dùng tài sản Highlands Coffee.

## 7. Kiểm thử, triển khai và bàn giao

- [ ] Viết test service/controller cho logic quan trọng: giá đơn, coupon, trạng thái đơn, phân quyền.
- [ ] Kiểm thử thủ công toàn bộ tiêu chí nghiệm thu.
- [ ] Kiểm tra lỗi validation, thông báo lỗi và loading state trên giao diện.
- [ ] Hoàn thiện API documentation và hướng dẫn cài đặt/chạy dự án.
- [ ] Deploy frontend (Vercel/Netlify) và backend/database (Render/Railway), hoặc chuẩn bị Docker/local demo.
- [ ] Kiểm tra cấu hình production: biến môi trường, CORS, URL API và tài khoản demo.
- [ ] Chuẩn bị báo cáo, ERD, sơ đồ kiến trúc và kịch bản demo.

## 8. Tiêu chí nghiệm thu cuối

- [ ] Đăng ký, đăng nhập, đăng xuất hoạt động.
- [ ] Khách xem danh mục, tìm kiếm và xem chi tiết sản phẩm.
- [ ] Giỏ hàng thêm/sửa/xóa đúng và tổng tiền đúng.
- [ ] Coupon chỉ áp dụng khi hợp lệ (nếu đã triển khai).
- [ ] Tạo đơn thành công và có mã/trang xác nhận.
- [ ] Khách không xem được đơn của người khác.
- [ ] Admin CRUD danh mục, sản phẩm, coupon (nếu đã triển khai).
- [ ] Admin cập nhật trạng thái đơn đúng luồng.
- [ ] Giao diện chính responsive.
- [ ] Không lộ mật khẩu; API admin được xác thực và phân quyền.

## 9. Kịch bản demo

- [ ] Admin đăng nhập, tạo danh mục và sản phẩm.
- [ ] Đăng nhập khách; xác nhận sản phẩm vừa tạo xuất hiện.
- [ ] Khách tìm sản phẩm, chọn size/số lượng (nếu có), thêm vào giỏ.
- [ ] Khách áp coupon hợp lệ (nếu có) và tạo đơn COD.
- [ ] Admin xác nhận, giao hàng và hoàn thành đơn.
- [ ] Khách mở lịch sử đơn, xác nhận trạng thái mới.

## 10. Nhật ký phiên làm việc

| Ngày | Hoàn thành | Việc tiếp theo | Ghi chú/vướng mắc |
| --- | --- | --- | --- |
| 2026-09-26 | Migrate backend; thêm JWT, BCrypt, CRUD admin, tìm kiếm catalog, tính phí giao hàng backend; build thành công bằng Maven Wrapper | UI tài khoản/quản trị và coupon | Không duy trì test source theo quyết định hiện tại |

## Gợi ý commit

- [ ] `chore: initialize frontend and spring boot backend`
- [ ] `feat(auth): add registration login and jwt authorization`
- [ ] `feat(admin): implement category and product management`
- [ ] `feat(catalog): add product listing search and detail pages`
- [ ] `feat(cart): implement cart item management and price calculation`
- [ ] `feat(order): create checkout and customer order history`
- [ ] `feat(admin): add order status management and dashboard`
- [ ] `test: add service and controller tests`
- [ ] `docs: add setup guide and API documentation`
