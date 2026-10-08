# Checklist dự án — Website đặt hàng cà phê

> **Cách dùng:** Khi hoàn tất một việc, đổi `- [ ]` thành `- [x]`, đồng thời ghi ngày/ghi chú ngắn ngay dưới mục đó nếu cần. Phiên làm việc sau phải đọc phần **Tiến độ hiện tại** và chỉ làm các mục chưa đánh dấu.
>
> **Phạm vi:** Lấy cảm hứng từ luồng đặt hàng cà phê, nhưng dùng tên, logo, hình ảnh và nội dung riêng; không sao chép nhận diện hay dữ liệu của Highlands Coffee.
>
> **Quy tắc code:** Ưu tiên code dễ đọc thay vì rút ngắn số dòng. Mỗi annotation, hàm, nhánh điều kiện, thao tác gán và thuộc tính builder cần trình bày trên dòng riêng khi việc gộp chúng làm khó đọc. Không viết nhiều câu lệnh Java trên cùng một dòng; giữ tên biến/hàm rõ nghĩa và format nhất quán.

## Tiến độ hiện tại

- [x] Khởi tạo: frontend Vanilla JS và backend Spring Boot 4.1.1 có Maven Wrapper.
- [x] MVP: các luồng catalog → giỏ hàng → checkout → quản lý đơn đã hoạt động.
- [x] Tính năng nâng cao giai đoạn 1: JWT, coupon, tùy chọn món, dashboard và bộ lọc đơn đã hoàn thành.
- [ ] Kiểm thử, triển khai và tài liệu: đã có unit test nền tảng, còn thiếu integration/E2E và môi trường production.
- [x] Luồng demo chức năng chính đã hoàn chỉnh; còn cần chạy lại checklist nghiệm thu trực quan trước khi bảo vệ.

**Ghi chú phiên gần nhất (2026-09-26):** Maven Wrapper build thành công. Catalog hỗ trợ lọc/tìm; đơn hàng lưu tạm tính, phí giao và tổng tiền do backend tính. Backend có JWT/BCrypt, phân quyền admin và CRUD danh mục/sản phẩm.

**Cập nhật 2026-10-08:** Hoàn thiện hủy đơn `PENDING` có kiểm tra chủ sở hữu; admin tìm/lọc đơn theo trạng thái, từ khóa và khoảng ngày, xem chi tiết bằng modal; storefront có live search, toast và responsive mobile. Thêm 5 unit test cho `OrderService`, `mvnw clean test` thành công.

---

## 1. Chốt phạm vi và quyết định

- [x] Chốt thương hiệu demo `Mộc Nhiên Coffee`; thay toàn bộ nhãn/URL ảnh Highlands bằng nội dung riêng và placeholder trung lập.
- [x] Chốt stack hiện tại: Vanilla JS + Spring Boot 4.1.1 + MySQL.
- [x] Chốt Java 17 theo cấu hình Maven hiện tại.
- [x] Hỗ trợ khách vãng lai đặt hàng; tài khoản dùng cho hồ sơ và khu vực quản trị.
- [x] Lưu giỏ khách vãng lai bằng `localStorage`.
- [x] Phí ship 15.000 VNĐ; miễn phí từ 200.000 VNĐ; cấu hình qua biến môi trường.
- [x] Chốt quy ước doanh thu dashboard: chỉ tính đơn `COMPLETED`.
- [x] Xác nhận ngoài phạm vi MVP: thanh toán thật, định vị/giao hàng thời gian thực, tích điểm, flash sale, đa kho, review, chatbot.

## 2. Thiết kế trước khi code

- [ ] Lập wireframe/Figma cho khách hàng: trang chủ, danh sách, chi tiết, giỏ, checkout, tài khoản.
- [ ] Lập wireframe/Figma cho admin: dashboard, danh mục, sản phẩm, coupon, đơn hàng, người dùng.
- [ ] Vẽ ERD và xác nhận quan hệ dữ liệu.
- [x] Định nghĩa enum `PERCENT`, `FIXED` cho coupon.
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
- [x] Đăng xuất ở frontend.
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
- [x] Khách xem lịch sử đơn theo tài khoản (trang `my-orders.html` & API `/api/v1/orders/my-orders`).
- [x] Admin xem danh sách, chi tiết đơn, lọc/tìm kiếm và cập nhật trạng thái.
- [x] Cho phép luồng `PENDING → CONFIRMED → DELIVERING → COMPLETED`.
- [x] Chỉ cho phép hủy từ `PENDING` hoặc `CONFIRMED`.
- [x] Chặn sửa đơn đã `COMPLETED` hoặc `CANCELLED`.
- [x] API tạo/tra cứu đơn và cập nhật trạng thái (hiện dùng `/api/v1/orders`).

### 4.6. Responsive và kiểm thử MVP

- [x] Đã triển khai responsive cho các trang chính trên mobile và desktop; còn cần QA trực quan đa thiết bị.
- [x] Kiểm thử luồng bắt buộc: sản phẩm → giỏ hàng → tạo đơn → admin đổi trạng thái.
- [x] Kiểm thử phân quyền và truy cập đơn hàng của người khác.
- [x] Sửa các lỗi blocker trước khi chuyển sang tính năng nâng cao.

## 5. Tính năng sau MVP

- [x] Coupon: CRUD admin.
- [x] Coupon: API `POST /coupons/validate`.
- [x] Coupon: kiểm tra active, thời hạn, giá trị đơn tối thiểu và chỉ một mã/đơn.
- [x] Coupon: giảm giá không vượt tạm tính; hỗ trợ `PERCENT` và `FIXED`.
- [x] Tuỳ chọn sản phẩm: size S/M/L, nóng/đá, phụ thu, ghi chú món.
- [x] Lưu `option_summary` tại giỏ và `order_items`.
- [x] Dashboard: tổng đơn, doanh thu theo quy ước đã chốt, số sản phẩm/người dùng, đơn mới nhất.
- [ ] Sắp xếp giá/mới nhất, phân trang hoặc “Xem thêm”.
- [ ] Upload ảnh Cloudinary.
- [ ] Newsletter chỉ lưu email (nếu còn thời gian).

## 6. Dữ liệu mẫu

- [x] Tạo 5–7 danh mục (6 danh mục: Cà phê, Trà, Đá xay, Sinh tố, Bánh, Combo).
- [x] Tạo 20–30 sản phẩm có ảnh, giá và mô tả ngắn (22 sản phẩm có cấu hình Size & Topping).
- [x] Tạo 2–3 khách hàng và 1 admin (`admin@mocnhien.vn` + 3 khách hàng test).
- [x] Tạo 2–3 coupon (`WELCOME10`, `MOCNHIEN20K`, `FREESHIP15K`).
- [x] Tạo 8–10 đơn ở nhiều trạng thái (9 đơn hàng ở các trạng thái PENDING, CONFIRMED, DELIVERING, COMPLETED, CANCELLED).
- [x] Xác nhận seed data phù hợp demo và không dùng tài sản Highlands Coffee.

## 7. Kiểm thử, triển khai và bàn giao

- [ ] Viết test service/controller cho logic quan trọng: giá đơn, coupon, trạng thái đơn, phân quyền.
- [ ] Kiểm thử thủ công toàn bộ tiêu chí nghiệm thu.
- [ ] Kiểm tra lỗi validation, thông báo lỗi và loading state trên giao diện.
- [ ] Hoàn thiện API documentation và hướng dẫn cài đặt/chạy dự án.
- [ ] Deploy frontend (Vercel/Netlify) và backend/database (Render/Railway), hoặc chuẩn bị Docker/local demo.
- [ ] Kiểm tra cấu hình production: biến môi trường, CORS, URL API và tài khoản demo.
- [ ] Chuẩn bị báo cáo, ERD, sơ đồ kiến trúc và kịch bản demo.

## 8. Tiêu chí nghiệm thu cuối

- [x] Đăng ký, đăng nhập, đăng xuất hoạt động.
- [x] Khách xem danh mục, tìm kiếm và xem chi tiết sản phẩm.
- [x] Giỏ hàng thêm/sửa/xóa đúng và tổng tiền đúng.
- [x] Coupon chỉ áp dụng khi hợp lệ (nếu đã triển khai).
- [x] Tạo đơn thành công và có mã/trang xác nhận.
- [x] Khách không xem được đơn của người khác.
- [x] Admin CRUD danh mục, sản phẩm, topping.
- [x] Admin cập nhật trạng thái đơn đúng luồng.
- [x] Giao diện chính có responsive; cần xác nhận lại bằng checklist kích thước màn hình trước khi bàn giao.
- [x] Không lộ mật khẩu; API admin được xác thực và phân quyền.

## 9. Kịch bản demo

- [x] Admin đăng nhập, tạo danh mục và sản phẩm.
- [x] Đăng nhập khách; xác nhận sản phẩm vừa tạo xuất hiện.
- [x] Khách tìm sản phẩm, chọn size/số lượng (nếu có), thêm vào giỏ.
- [x] Khách áp coupon hợp lệ (nếu có) và tạo đơn COD.
- [x] Admin xác nhận, giao hàng và hoàn thành đơn.
- [x] Khách mở lịch sử đơn, xác nhận trạng thái mới.

## 10. Nhật ký phiên làm việc

| Ngày | Hoàn thành | Việc tiếp theo | Ghi chú/vướng mắc |
| --- | --- | --- | --- |
| 2026-09-26 | Migrate backend; thêm JWT, BCrypt, CRUD admin, tìm kiếm catalog, tính phí giao hàng backend; build thành công bằng Maven Wrapper | UI tài khoản/quản trị và coupon | Không duy trì test source theo quyết định hiện tại |
| 2026-09-29 | 1. Implement Auth UI (`login.html`, `register.html`), lưu Token JWT, dynamic User Header, đăng xuất.<br>2. Thêm API `/api/v1/orders/my-orders`, liên kết đơn hàng với `AppUser` và tạo trang lịch sử đơn cá nhân (`my-orders.html`).<br>3. Xây dựng giao diện Tab Admin CMS (`admin.html` & `admin.js`) với đầy đủ Form Modal CRUD Sản phẩm, Danh mục, Topping & Auth Guard cho `ROLE_ADMIN`. | Module 5 (Báo cáo Thống kê Doanh thu Admin) & Module 6 (Bổ sung Tùy chọn Mức đá / Mức đường) | Biên dịch Maven thành công (`BUILD SUCCESS`, 51 file source Java compiled). |
| 2026-10-05 | 1. Implement Module 5 (Admin Dashboard / Stats): API `GET /api/v1/admin/dashboard` trả về tổng doanh thu đơn COMPLETED, tổng số đơn, sản phẩm, người dùng và danh sách đơn hàng gần đây; bổ sung Tab Thống Kê trên `admin.html`.<br>2. Implement Module Product Options: Bổ sung Mức đá, Mức đường và Ghi chú món vào entity `OrderItem`, DTO `OrderItemRequest`, Modal Chi tiết sản phẩm (`index.html`), Giỏ hàng, Checkout, Tra cứu đơn & Lịch sử đơn.<br>3. Implement Coupon System: Admin CRUD, Customer Validation API (`POST /api/v1/coupons/validate`), áp mã giảm giá tại Checkout, trừ tiền tự động backend.<br>4. Hoàn thành Mốc 6 (Dữ liệu mẫu): Khởi tạo 6 danh mục, 22 sản phẩm, 4 người dùng (1 admin, 3 khách), 3 mã coupon, 9 đơn hàng mẫu ở đa dạng trạng thái (`PENDING`, `CONFIRMED`, `DELIVERING`, `COMPLETED`, `CANCELLED`). | Kiểm thử thủ công, bổ sung bộ lọc sắp xếp giá/mới nhất, chuẩn bị tài liệu bàn giao. | Maven Wrapper build thành công 100% (`BUILD SUCCESS`). Áp dụng triệt để nguyên tắc code sạch (Lazy Senior Dev mode) & format nhất quán. |
| 2026-10-08 | Khách tự hủy đơn `PENDING` có kiểm tra chủ sở hữu; admin tìm/lọc đơn theo mã, tên, SĐT, trạng thái và khoảng ngày; modal chi tiết đơn; live search; toast; responsive mobile; thêm 5 unit test `OrderService`. | Thực hiện mốc P0 trong roadmap mở rộng: migration database, DTO response, chuẩn hóa lỗi API và integration test. | `mvnw clean test` thành công, 5/5 test đạt; các trang frontend trả HTTP 200. |

---

## 11. Roadmap mở rộng từ phiên tiếp theo

### 11.1. Nguyên tắc ưu tiên

- `P0 — Ổn định`: xử lý rủi ro dữ liệu, bảo mật, hợp đồng API và kiểm thử trước khi thêm tính năng lớn.
- `P1 — Hoàn thiện sản phẩm`: nâng cấp trải nghiệm đặt hàng và công cụ vận hành cho admin.
- `P2 — Nâng cao`: chỉ triển khai sau khi P0/P1 có test và luồng demo ổn định.
- Mỗi phiên ưu tiên hoàn thành một lát cắt xuyên suốt: database → repository/service → API → giao diện → test → tài liệu.
- Không bổ sung tính năng chỉ có giao diện giả; mọi dữ liệu nghiệp vụ quan trọng phải được backend xác nhận.

### 11.2. Mốc P0 — Ổn định kiến trúc và dữ liệu

#### Database và migration

- [ ] Tích hợp Flyway; tạo migration khởi tạo schema thay cho phụ thuộc hoàn toàn vào Hibernate auto-DDL.
- [ ] Tạo migration bổ sung index cho `orders.order_code`, `orders.created_at`, `orders.status`, `users.email`, `products.name` và `coupons.code`.
- [ ] Chuyển seed data demo thành profile riêng (`dev` hoặc `demo`), không tự động nạp trong production.
- [ ] Quy định rõ `spring.jpa.hibernate.ddl-auto` cho từng profile: `update/create-drop` ở local test, `validate` ở production.
- [ ] Sao lưu/khôi phục database bằng script hoặc hướng dẫn trong `docs/`.

#### Hợp đồng API và DTO

- [ ] Không trả trực tiếp JPA Entity từ controller; tạo response DTO cho Product, Order, OrderItem, Category, Coupon và Dashboard.
- [ ] Chuẩn hóa `ApiResponse`: `success`, `message`, `data`, `errorCode`, `timestamp`, `path`.
- [ ] Tách lỗi nghiệp vụ thành mã ổn định như `ORDER_NOT_FOUND`, `INVALID_ORDER_STATUS`, `COUPON_EXPIRED`, `ACCESS_DENIED`.
- [ ] Bổ sung handler cho lỗi JSON sai định dạng, query parameter sai kiểu, access denied và JWT hết hạn.
- [ ] Chuẩn hóa HTTP status: `201` khi tạo, `204` khi xóa, `400` validation, `401` chưa đăng nhập, `403` thiếu quyền, `404` không tìm thấy, `409` xung đột.
- [ ] Hoàn thiện OpenAPI/Swagger: mô tả endpoint, ví dụ request/response và yêu cầu Bearer JWT.

#### Bảo mật nền tảng

- [ ] Rà lại toàn bộ `SecurityConfig`; gom route public/customer/admin theo nhóm rõ ràng và thêm test cho từng nhóm.
- [ ] Không cho khách vãng lai tra cứu đơn chỉ bằng mã ngắn; yêu cầu thêm số điện thoại hoặc token tra cứu đơn.
- [ ] Chuẩn hóa JWT expiration, issuer, clock skew và phản hồi khi token hết hạn.
- [ ] Bổ sung giới hạn tần suất cho đăng nhập, đăng ký, validate coupon và tra cứu đơn.
- [ ] Kiểm tra/escape dữ liệu người dùng trước khi render HTML; loại bỏ các vị trí `innerHTML` có nguy cơ XSS.
- [ ] Thêm security headers phù hợp: CSP, `X-Content-Type-Options`, `Referrer-Policy` và frame protection.
- [ ] Kiểm tra `.env`, secret, tài khoản demo và dữ liệu nhạy cảm không được commit.

#### Kiểm thử P0

- [ ] Unit test tính giá: size, topping, số lượng, phí giao, miễn phí giao và làm tròn tiền.
- [ ] Unit test coupon: hết hạn, chưa bắt đầu, minimum order, percent/fixed, max discount và mã không tồn tại.
- [ ] Unit test toàn bộ chuyển trạng thái đơn hợp lệ/không hợp lệ.
- [ ] Controller test cho auth, product, coupon, order và admin endpoints bằng MockMvc.
- [ ] Repository/integration test cho truy vấn lọc đơn và tìm kiếm sản phẩm.
- [ ] Dùng Testcontainers MySQL hoặc database test tương thích để kiểm tra migration và JPA mapping.
- [ ] Đặt ngưỡng coverage tối thiểu cho service nghiệp vụ quan trọng; đề xuất ban đầu `70%` line coverage.

### 11.3. Mốc P1 — Storefront và trải nghiệm đặt hàng

#### Trang chủ và khám phá sản phẩm

- [ ] Hoàn thiện trang chủ gồm hero banner, danh mục nổi bật, món bán chạy, sản phẩm mới, ưu đãi và footer đầy đủ.
- [ ] Thêm API/tiêu chí lấy “món bán chạy” từ order items của đơn `COMPLETED`.
- [ ] Thêm nhãn sản phẩm: `Mới`, `Bán chạy`, `Tạm hết`, phần trăm giảm giá.
- [ ] Bổ sung sắp xếp theo giá tăng/giảm, mới nhất và bán chạy.
- [ ] Bổ sung phân trang hoặc nút “Xem thêm”; giữ lại bộ lọc trên URL để chia sẻ/refresh không mất trạng thái.
- [ ] Thêm trạng thái skeleton loading, empty state và nút thử lại khi API lỗi.
- [ ] Thêm quick-view thân thiện bàn phím, đóng modal bằng `Escape`, focus trap và ARIA label.

#### Sản phẩm và giỏ hàng

- [ ] Mô hình hóa nhóm tùy chọn sản phẩm thay vì hard-code đá/đường; cho admin cấu hình option và phụ thu.
- [ ] Hỗ trợ trạng thái tồn kho/tạm hết và chặn thêm món không còn bán.
- [ ] Cho phép sửa tùy chọn của một dòng hàng ngay từ giỏ.
- [ ] Ghép các dòng giỏ giống hệt sản phẩm + size + topping + đá + đường + ghi chú.
- [ ] Hiển thị thanh tiến độ “còn bao nhiêu để được miễn phí giao hàng”.
- [ ] Thêm danh sách sản phẩm đã xem gần đây bằng `localStorage`.
- [ ] Thêm nút yêu thích; giai đoạn đầu lưu local, sau đó đồng bộ theo tài khoản.

#### Checkout và hậu mãi

- [ ] Tách bước checkout: thông tin nhận hàng → xác nhận đơn → kết quả đặt hàng.
- [ ] Validate số điện thoại Việt Nam, độ dài địa chỉ và ghi chú ở cả frontend/backend.
- [ ] Lưu nhiều địa chỉ nhận hàng cho tài khoản; cho chọn địa chỉ mặc định.
- [ ] Thêm lựa chọn `Giao tận nơi` và `Nhận tại cửa hàng` với mô hình dữ liệu phù hợp.
- [ ] Hiển thị thời gian giao dự kiến dạng cấu hình, không cam kết định vị thời gian thực.
- [ ] Cho khách nhập lý do hủy đơn; lưu thời điểm và người thực hiện hủy.
- [ ] Thêm “Đặt lại đơn” từ lịch sử; backend phải kiểm tra lại giá và trạng thái sản phẩm.
- [ ] Tạo hóa đơn/phiếu đơn hàng có thể in hoặc xuất PDF sau khi đặt thành công.

#### Tài khoản khách hàng

- [ ] Hoàn thiện trang hồ sơ: cập nhật họ tên, SĐT, đổi mật khẩu và quản lý địa chỉ.
- [ ] Thêm xác minh email hoặc luồng quên mật khẩu bằng token có thời hạn.
- [ ] Thêm phân trang và lọc lịch sử đơn theo trạng thái/thời gian.
- [ ] Hiển thị timeline trạng thái đơn và lịch sử thay đổi trạng thái.
- [ ] Thêm thông báo trong ứng dụng khi trạng thái đơn thay đổi.

### 11.4. Mốc P1 — Quản trị và báo cáo

#### Quản lý vận hành

- [ ] Phân trang server-side cho bảng đơn, sản phẩm, coupon và người dùng.
- [ ] Thêm bộ lọc sản phẩm theo danh mục/trạng thái; tìm kiếm và chỉnh sửa hàng loạt trạng thái hiển thị.
- [ ] Thêm quản lý người dùng: xem danh sách, khóa/mở tài khoản; không cho admin tự hạ quyền admin cuối cùng.
- [ ] Thêm lý do hủy, ghi chú nội bộ và lịch sử ai đã cập nhật trạng thái đơn.
- [ ] Ngăn cập nhật đồng thời bằng optimistic locking (`@Version`) cho Order/Product/Coupon.
- [ ] Thêm export CSV danh sách đơn theo bộ lọc hiện tại.

#### Dashboard phân tích

- [ ] Bộ lọc dashboard theo ngày/tuần/tháng và khoảng ngày tùy chọn.
- [ ] Biểu đồ doanh thu theo ngày; chỉ tính đơn `COMPLETED`.
- [ ] Thống kê số đơn và tỷ lệ hủy theo trạng thái.
- [ ] Top sản phẩm, danh mục và topping bán chạy theo số lượng/doanh thu.
- [ ] Giá trị đơn trung bình, doanh thu sau giảm giá và tổng giá trị coupon đã dùng.
- [ ] So sánh kỳ hiện tại với kỳ trước; ghi rõ công thức trên giao diện.
- [ ] Tối ưu truy vấn aggregate, tránh tải toàn bộ order vào bộ nhớ để tính dashboard.

#### Coupon và chiến dịch

- [ ] Bổ sung ngày bắt đầu/kết thúc, số lượt dùng toàn hệ thống và mỗi khách hàng.
- [ ] Cho phép coupon chỉ áp dụng cho danh mục/sản phẩm được chọn.
- [ ] Lưu lịch sử sử dụng coupon theo order/user để chống dùng vượt giới hạn.
- [ ] Thêm banner ưu đãi lấy từ backend, có thời gian hiển thị và trạng thái active.
- [ ] Flash sale chỉ triển khai sau khi có tồn kho, giới hạn mua và xử lý concurrency rõ ràng.

### 11.5. Mốc P1 — Nâng cấp công nghệ frontend

- [ ] Tách JavaScript theo module ES: `api`, `auth`, `catalog`, `cart`, `checkout`, `orders`, `admin`, `ui`.
- [ ] Không dùng inline `onclick`; chuyển sang `addEventListener` và event delegation.
- [ ] Đưa `API_BASE_URL` sang file cấu hình theo môi trường, không hard-code localhost trong source production.
- [ ] Tạo component/hàm render dùng chung cho header, footer, status badge, loading, empty state, modal và toast.
- [ ] Thêm ESLint + Prettier; cấu hình script kiểm tra cú pháp/format trong CI.
- [ ] Viết test frontend cho CartManager, định dạng tiền, query filter và các hàm render an toàn.
- [ ] Đánh giá migration sang Vite + TypeScript sau khi Vanilla JS đã được module hóa; không rewrite toàn bộ chỉ để đổi framework.
- [ ] Nếu chuyển framework, ưu tiên migration từng trang và giữ nguyên REST API hiện tại.

### 11.6. Mốc P1 — Media và nội dung

- [ ] Thay placeholder bằng bộ ảnh sản phẩm riêng, tối ưu WebP/AVIF và có ảnh fallback.
- [ ] Upload ảnh qua Cloudinary hoặc S3-compatible storage; backend chỉ lưu URL và metadata.
- [ ] Validate loại file, dung lượng, kích thước; không cho upload SVG/HTML không tin cậy.
- [ ] Thêm trường alt text và kiểm tra tỷ lệ ảnh nhất quán.
- [ ] Tạo trang chính sách đặt hàng, bảo mật, thanh toán và liên hệ bằng nội dung demo phù hợp.
- [ ] Thêm metadata SEO cơ bản, favicon, Open Graph và sitemap nếu triển khai public.

### 11.7. Mốc P2 — Tính năng nâng cao tùy thời gian

- [ ] Tích điểm/đổi điểm với sổ giao dịch điểm bất biến; không chỉ lưu một số dư có thể sửa trực tiếp.
- [ ] Hạng thành viên và ưu đãi theo hạng.
- [ ] Thanh toán sandbox VNPay/MoMo; xác minh chữ ký callback và idempotency trước khi cân nhắc thanh toán thật.
- [ ] Email thông báo xác nhận đơn và thay đổi trạng thái qua hàng đợi/background job.
- [ ] Đánh giá nhiều cửa hàng/khu vực giao hàng; chưa triển khai đa kho nếu chưa chốt nghiệp vụ tồn kho.
- [ ] Review sản phẩm chỉ cho tài khoản có đơn `COMPLETED` chứa sản phẩm tương ứng.
- [ ] PWA: installable, cache static asset và trang offline; không cache dữ liệu tài khoản nhạy cảm.
- [ ] Chatbot chỉ dùng cho FAQ/menu; không tự ý đặt hoặc hủy đơn nếu chưa có bước xác nhận rõ ràng.

## 12. DevOps, quan sát hệ thống và triển khai

### 12.1. Môi trường và đóng gói

- [ ] Tạo profile `dev`, `test`, `demo`, `prod` và tài liệu biến môi trường tương ứng.
- [ ] Dockerfile multi-stage cho backend; cấu hình chạy non-root và healthcheck.
- [ ] Docker Compose cho local gồm MySQL + backend; frontend có thể chạy static server riêng.
- [ ] Không đóng gói `.env`, secret hoặc dữ liệu test vào image.
- [ ] Thêm Spring Boot Actuator; chỉ public health endpoint tối thiểu, bảo vệ metrics/management endpoint.

### 12.2. CI/CD

- [ ] GitHub Actions chạy `mvnw clean test`, kiểm tra JavaScript, format và build trên mỗi pull request.
- [ ] Cache Maven an toàn để giảm thời gian CI.
- [ ] Thêm bước scan dependency và secret; xử lý mức nghiêm trọng cao trước khi deploy.
- [ ] Tạo artifact/version rõ ràng theo commit SHA hoặc release tag.
- [ ] Deploy staging trước production; smoke test health, catalog và tạo đơn demo sau deploy.
- [ ] Chuẩn bị rollback về phiên bản trước và hướng dẫn khôi phục database.

### 12.3. Logging và monitoring

- [ ] Chuẩn hóa SLF4J logging; không log password, JWT, địa chỉ đầy đủ hoặc dữ liệu nhạy cảm.
- [ ] Thêm correlation/request ID từ frontend đến backend và đưa vào log.
- [ ] Ghi audit log cho đăng nhập thất bại, thay đổi sản phẩm/coupon và cập nhật trạng thái đơn.
- [ ] Theo dõi latency, tỷ lệ lỗi, số request, connection pool và truy vấn chậm.
- [ ] Thiết lập cảnh báo tối thiểu cho ứng dụng down, lỗi 5xx tăng cao và database không kết nối được.

## 13. Ma trận phát hành đề xuất

| Mốc | Phạm vi bắt buộc | Điều kiện hoàn thành |
| --- | --- | --- |
| `v0.2 — Stabilize` | Flyway, DTO response, error contract, security route, unit/controller test | Build sạch; migration chạy trên DB mới; không trả Entity/password; test nghiệp vụ chính đạt |
| `v0.3 — Storefront` | Trang chủ hoàn chỉnh, sort/pagination, cart UX, checkout validation, reorder | Luồng mobile/desktop đạt; backend xác nhận mọi giá; không có blocker trong checklist thủ công |
| `v0.4 — Operations` | Admin pagination, audit trạng thái, dashboard theo thời gian, CSV | Số liệu đối chiếu đúng DB; truy vấn không N+1; role admin được kiểm thử |
| `v0.5 — Production Ready` | Docker, CI/CD, healthcheck, logging, staging, tài liệu | Deploy staging lặp lại được; smoke test đạt; có rollback và tài liệu vận hành |
| `v1.0 — Demo/Release` | Hoàn thiện báo cáo, ERD, API docs, video/kịch bản demo | Nghiệm thu toàn bộ tiêu chí; không còn lỗi nghiêm trọng/cao |

## 14. Definition of Done cho mỗi tính năng

- [ ] Yêu cầu và trường hợp lỗi được ghi rõ trước khi code.
- [ ] Có migration nếu thay đổi schema; migration chạy được trên database trống và database hiện có.
- [ ] Backend validate input, kiểm tra quyền và không tin giá trị tiền do frontend gửi lên.
- [ ] API dùng DTO, status code và error code đúng quy ước.
- [ ] Frontend có loading, empty, success và error state; dùng được bằng bàn phím ở luồng chính.
- [ ] Có unit/integration test phù hợp với rủi ro của thay đổi.
- [ ] Chạy `mvnw clean test`, kiểm tra JavaScript và `git diff --check` thành công.
- [ ] Cập nhật README/API docs/roadmap và thêm một dòng vào nhật ký phiên.
- [ ] Không đưa secret, tài sản thương hiệu bên thứ ba hoặc dữ liệu cá nhân thật vào repository.

## 15. Checklist QA trước khi bảo vệ

### Trình duyệt và responsive

- [ ] Kiểm tra Chrome/Edge bản hiện hành ở kích thước 360 px, 768 px, 1024 px và desktop ≥ 1440 px.
- [ ] Không tràn ngang ngoài bảng admin có chủ đích; modal và drawer không vượt viewport.
- [ ] Kiểm tra focus, tab order, đóng modal bằng Escape và độ tương phản chữ/nút.
- [ ] Ảnh có fallback; giao diện vẫn dùng được khi ảnh hoặc API tải chậm.

### Luồng khách hàng

- [ ] Khách vãng lai duyệt menu, tìm/lọc, tùy chọn món, sửa giỏ và checkout COD.
- [ ] Đăng ký/đăng nhập sai và đúng; token hết hạn đưa người dùng về trạng thái an toàn.
- [ ] Coupon hợp lệ/không hợp lệ/hết hạn/không đạt minimum order.
- [ ] Hủy đơn PENDING của chính mình; không hủy được đơn người khác hoặc đơn đã xác nhận.
- [ ] Đặt lại đơn khi sản phẩm đã đổi giá hoặc tạm ẩn.

### Luồng admin

- [ ] CRUD category/product/topping/coupon với dữ liệu hợp lệ và không hợp lệ.
- [ ] Lọc đơn theo từng điều kiện và tổ hợp điều kiện; kiểm tra biên ngày cuối được tính đủ.
- [ ] Thử toàn bộ chuyển trạng thái hợp lệ và chuyển trạng thái bị cấm.
- [ ] Dashboard đối chiếu thủ công với dữ liệu đơn `COMPLETED`.
- [ ] Tài khoản customer không gọi được bất kỳ admin endpoint nào.

### Phi chức năng

- [ ] Không có lỗi console nghiêm trọng hoặc API 5xx trong luồng demo.
- [ ] Catalog và dashboard không phát sinh N+1 query đáng kể.
- [ ] Trang chính đạt mục tiêu Lighthouse đề xuất: Performance ≥ 80, Accessibility ≥ 90, Best Practices ≥ 90.
- [ ] Dữ liệu tiền dùng `BigDecimal` ở backend và định dạng VND nhất quán ở frontend.
- [ ] Thử khởi động bằng hướng dẫn README trên môi trường sạch.

## 16. Backlog chưa cam kết

- [ ] Đa ngôn ngữ Việt/Anh.
- [ ] Dark mode.
- [ ] Bản đồ và tự động tính phí theo khoảng cách.
- [ ] Quản lý tồn kho theo nguyên liệu.
- [ ] Kiosk/POS tại cửa hàng.
- [ ] Ứng dụng mobile native.
- [ ] Kiến trúc microservices.

> Các mục backlog chưa cam kết không được ưu tiên hơn P0/P1. Đặc biệt, chưa chuyển microservices hoặc rewrite frontend khi monolith hiện tại vẫn đáp ứng quy mô đồ án.

---

## 17. Kế hoạch tích hợp RAG gợi ý món theo lịch sử mua hàng

### 17.1. Mục tiêu và phạm vi

- [ ] Xây dựng khu vực “Gợi ý dành cho bạn” cho khách hàng đã đăng nhập và có lịch sử mua hàng.
- [ ] Gợi ý món dựa trên lịch sử đơn `COMPLETED`, danh mục thường mua, size, topping, mức đá/đường, khung giờ và mức giá quen thuộc.
- [ ] Dùng RAG để truy xuất các món phù hợp từ catalog hiện hành và tạo lời giải thích ngắn, ví dụ: “Bạn thường chọn trà ít đường vào buổi chiều”.
- [ ] Chỉ gợi ý sản phẩm đang active, còn bán và có dữ liệu giá hợp lệ tại thời điểm trả response.
- [ ] Không để LLM tự tạo tên món, giá, coupon hoặc thuộc tính không tồn tại trong database.
- [ ] Có chiến lược fallback không dùng AI cho khách chưa đăng nhập, lịch sử quá ít hoặc dịch vụ AI gặp lỗi.
- [ ] Xem đây là `P2`; chỉ triển khai sau khi hợp đồng API, DTO, bảo mật và integration test ở P0 ổn định.

### 17.2. Kiến trúc hybrid recommendation + RAG

```text
Order history ──> Preference Profile ──┐
                                      ├──> Candidate Retrieval ──> Rule Filter ──> Ranking ──> Top N
Product catalog ──> Embedding/Vector ─┘                                      │
                                                                               v
                                                              LLM explanation (optional)
                                                                               │
                                                                               v
                                                               Recommendation API/UI
```

- [ ] `Preference Profile`: tổng hợp sở thích có cấu trúc từ lịch sử mua, không gửi toàn bộ đơn thô cho LLM.
- [ ] `Candidate Retrieval`: tìm ứng viên bằng vector similarity trên mô tả sản phẩm kết hợp bộ lọc category/giá/thời gian.
- [ ] `Rule Filter`: loại sản phẩm inactive, hết hàng, không phù hợp điều kiện dị ứng/kiêng nếu sau này có dữ liệu đó.
- [ ] `Ranking`: kết hợp semantic score, tần suất mua, độ mới của lịch sử, độ đa dạng và độ phổ biến.
- [ ] `LLM explanation`: chỉ diễn giải các ứng viên đã được backend chọn; không có quyền quyết định giá hoặc tạo order.
- [ ] Pha MVP RAG chưa cần agent hoặc framework phức tạp; ưu tiên pipeline có thể quan sát và kiểm thử được.

### 17.3. Quyết định công nghệ cần chốt

- [ ] Chọn nhà cung cấp embedding/LLM thông qua interface riêng để có thể thay đổi mà không sửa nghiệp vụ.
- [ ] Chọn vector store:
  - Phương án A: PostgreSQL + `pgvector` nếu chấp nhận chuyển/đặt thêm kho dữ liệu vector.
  - Phương án B: Qdrant/Weaviate chạy riêng khi cần scale hoặc filter vector tốt hơn.
  - Phương án C: lưu embedding bên ngoài MySQL trong giai đoạn thử nghiệm; không cố lưu vector dạng chuỗi trong bảng sản phẩm chính.
- [ ] Giữ MySQL là nguồn dữ liệu nghiệp vụ chính; vector store chỉ là read model phục vụ truy xuất.
- [ ] Tạo abstraction `EmbeddingProvider`, `VectorProductStore`, `RecommendationService` và `ExplanationGenerator`.
- [ ] Cấu hình endpoint, model, API key, timeout và feature flag qua biến môi trường.
- [ ] Không commit API key; không expose provider key xuống frontend.

### 17.4. Mô hình dữ liệu đề xuất

- [ ] Bổ sung metadata sản phẩm phục vụ retrieval: `tags`, `flavor_notes`, `drink_type`, `caffeine_level`, `served_hot`, `served_cold`, `is_food`.
- [ ] Tạo `product_embeddings` hoặc collection vector gồm:
  - `product_id`
  - `embedding`
  - `embedding_model`
  - `content_hash`
  - `catalog_version`
  - `updated_at`
- [ ] Tạo `customer_preference_profiles` gồm:
  - `user_id`
  - danh mục/tags ưa thích
  - khoảng giá thường mua
  - size/topping/đá/đường thường chọn
  - khung giờ mua phổ biến
  - danh sách sản phẩm mua gần đây
  - `last_computed_at`
- [ ] Tạo `recommendation_events` để đo hiệu quả: `IMPRESSION`, `CLICK`, `ADD_TO_CART`, `PURCHASE`, `DISMISS`.
- [ ] Lưu `request_id`, `user_id`, danh sách ứng viên, score, phiên bản thuật toán và thời điểm; không lưu prompt chứa dữ liệu nhạy cảm.
- [ ] Cân nhắc trường `ai_recommendation_opt_out` trong hồ sơ người dùng.

### 17.5. Chuẩn bị dữ liệu và tạo embedding

- [ ] Chuẩn hóa văn bản index cho mỗi sản phẩm từ tên + danh mục + mô tả + tags + flavor notes + khoảng giá.
- [ ] Không đưa giá hiện tại vào embedding nếu giá thay đổi thường xuyên; dùng giá làm metadata filter/ranking.
- [ ] Tạo embedding khi sản phẩm được tạo/cập nhật và re-index khi `content_hash` thay đổi.
- [ ] Tạo batch job re-index toàn bộ catalog khi đổi embedding model.
- [ ] Đánh version embedding để không trộn vector từ các model/kích thước khác nhau.
- [ ] Có trạng thái retry/dead-letter cho sản phẩm index thất bại.
- [ ] Kiểm tra tiếng Việt có dấu/không dấu, tên viết tắt và từ đồng nghĩa như “cà phê sữa”, “bạc xỉu”, “ít ngọt”.

### 17.6. Xây dựng hồ sơ sở thích từ lịch sử mua

- [ ] Chỉ dùng đơn `COMPLETED`; không tính đơn `CANCELLED` và tránh dùng đơn test/admin seed ở production.
- [ ] Áp dụng trọng số theo độ mới, ví dụ đơn gần đây có trọng số cao hơn đơn cũ.
- [ ] Tăng trọng số cho món mua lặp lại; giảm dominance để kết quả không chỉ trả đúng một món quen thuộc.
- [ ] Tổng hợp sở thích theo category, product tags, giá, size, topping, đá/đường và khung giờ.
- [ ] Đặt ngưỡng lịch sử tối thiểu, ví dụ 2–3 đơn hoàn thành trước khi cá nhân hóa sâu.
- [ ] Với cold start, dùng món bán chạy theo khung giờ + danh mục người dùng đang xem + mức giá phổ biến.
- [ ] Cho phép người dùng chọn “Không quan tâm” để điều chỉnh hồ sơ và loại món khỏi lần gợi ý tiếp theo.
- [ ] Không suy luận dữ liệu nhạy cảm như sức khỏe, tôn giáo hoặc tình trạng cá nhân từ lịch sử đồ uống.

### 17.7. Retrieval, lọc và xếp hạng

- [ ] Sinh query retrieval từ preference profile có cấu trúc; không nối trực tiếp dữ liệu người dùng vào prompt tự do.
- [ ] Lấy khoảng 20–50 ứng viên vector trước khi filter/rank; chỉ trả 4–8 món cuối cùng.
- [ ] Lọc bắt buộc bằng database hiện hành: `is_active`, tồn kho, category, giá và phạm vi phục vụ.
- [ ] Loại sản phẩm vừa mua quá nhiều lần nếu mục tiêu là khám phá; giữ một phần kết quả “mua lại món quen”.
- [ ] Công thức ranking ban đầu có thể gồm:
  - `40%` semantic similarity
  - `25%` affinity từ lịch sử mua
  - `15%` recency/context theo khung giờ
  - `10%` popularity
  - `10%` diversity/novelty
- [ ] Đưa toàn bộ trọng số vào cấu hình và lưu `algorithm_version` để đánh giá/rollback.
- [ ] Không dùng coupon/giảm giá làm tín hiệu quá mạnh khiến hệ thống chỉ ưu tiên món đang khuyến mãi.
- [ ] Thêm diversity rule: không để toàn bộ Top N thuộc cùng một sản phẩm hoặc một category nếu có lựa chọn phù hợp khác.

### 17.8. Sinh giải thích bằng RAG

- [ ] Prompt chỉ nhận preference summary đã ẩn dữ liệu cá nhân và danh sách ứng viên đã được backend xác thực.
- [ ] Ép output theo JSON schema: `productId`, `reason`, `confidenceLabel`; từ chối product ID ngoài candidate list.
- [ ] Giới hạn lời giải thích 1 câu/món, thân thiện và không khẳng định quá mức.
- [ ] Ví dụ lý do hợp lệ: danh mục thường mua, mức ngọt/đá tương tự, khoảng giá gần, phù hợp buổi sáng/chiều.
- [ ] Không nêu thông tin gây cảm giác theo dõi quá chi tiết như thời gian/địa chỉ cụ thể của đơn cũ.
- [ ] Nếu LLM timeout hoặc JSON không hợp lệ, dùng explanation template do backend tạo từ các feature ranking.
- [ ] Cache explanation theo `user profile version + candidate IDs + algorithm version` trong thời gian ngắn.

### 17.9. API dự kiến

- [ ] `GET /api/v1/recommendations/me?limit=6&context=HOME`: lấy gợi ý cá nhân hóa cho người đăng nhập.
- [ ] `GET /api/v1/recommendations/popular?limit=6`: fallback public cho cold start.
- [ ] `POST /api/v1/recommendations/events`: ghi nhận impression/click/add-to-cart/dismiss.
- [ ] `POST /api/v1/admin/ai/products/reindex`: re-index catalog, chỉ dành cho admin hoặc internal job.
- [ ] `GET /api/v1/admin/ai/index-status`: xem số sản phẩm indexed/failed/stale.
- [ ] Response gợi ý chỉ trả Product DTO hiện hành cùng `reason`, `source`, `algorithmVersion`; không trả vector hoặc prompt.
- [ ] Thêm timeout tổng, circuit breaker và fallback để lỗi AI không làm hỏng trang chủ/checkout.
- [ ] Rate limit API recommendation theo user/IP và cache kết quả hợp lý.

### 17.10. Giao diện người dùng

- [ ] Thêm section “Gợi ý dành cho bạn” trên trang chủ sau khi đăng nhập.
- [ ] Hiển thị lý do ngắn dưới mỗi card và nhãn “Gợi ý cá nhân hóa”.
- [ ] Thêm nút “Vì sao gợi ý món này?” và “Không quan tâm”.
- [ ] Cho phép tắt cá nhân hóa trong trang hồ sơ; khi tắt chuyển sang món phổ biến chung.
- [ ] Không block giao diện trong lúc chờ AI; render catalog bình thường và tải recommendation bất đồng bộ.
- [ ] Có skeleton/empty/error state riêng; lỗi recommendation không hiện lỗi toàn trang.
- [ ] Theo dõi add-to-cart/purchase từ recommendation bằng `requestId` để đo conversion chính xác.

### 17.11. Bảo mật, riêng tư và chi phí

- [ ] Không gửi họ tên, email, số điện thoại, địa chỉ, JWT hoặc ghi chú giao hàng sang embedding/LLM provider.
- [ ] Chỉ gửi user ID giả danh/pseudonymous nếu thật sự cần; mặc định không gửi định danh người dùng.
- [ ] Tài liệu hóa dữ liệu nào được sử dụng cho cá nhân hóa và cách người dùng opt-out.
- [ ] Thiết lập retention cho recommendation events và xóa dữ liệu liên quan khi xóa tài khoản.
- [ ] Log token usage, latency và chi phí theo request nhưng không log prompt chứa hồ sơ người dùng.
- [ ] Đặt ngân sách/ngưỡng chi phí hằng ngày; tự động chuyển sang template explanation khi vượt ngưỡng.
- [ ] Chống prompt injection từ mô tả sản phẩm: coi catalog text là dữ liệu, không phải instruction; dùng schema output và allowlist product ID.
- [ ] Không cho RAG gọi trực tiếp API tạo/hủy đơn; mọi thao tác mua hàng vẫn qua luồng xác nhận hiện tại.

### 17.12. Đánh giá chất lượng

- [ ] Tạo tập đánh giá offline từ dữ liệu demo/ẩn danh: che món cuối và kiểm tra hệ thống có gợi ý lại đúng nhóm sở thích không.
- [ ] Chỉ số offline: Precision@K, Recall@K, NDCG@K, catalog coverage và diversity.
- [ ] Chỉ số online: CTR, add-to-cart rate, purchase conversion, doanh thu/phiên và tỷ lệ dismiss.
- [ ] So sánh với baseline không AI: món bán chạy toàn hệ thống và món phổ biến theo category.
- [ ] Không chấp nhận RAG nếu không vượt baseline hoặc làm giảm diversity đáng kể.
- [ ] Thực hiện A/B test có feature flag; nhóm control dùng baseline, nhóm treatment dùng hybrid RAG.
- [ ] Thu thập feedback “Hữu ích/Không phù hợp” để phân tích, chưa dùng tự động fine-tune trong giai đoạn đầu.

### 17.13. Kiểm thử RAG

- [ ] Unit test tạo preference profile, recency weight, candidate filter và ranking score.
- [ ] Test đảm bảo mọi recommendation ID đều tồn tại, active và đúng giá từ database.
- [ ] Contract test cho embedding/LLM provider bằng fake adapter; CI không gọi API trả phí thật.
- [ ] Integration test vector search bằng container/local vector store.
- [ ] Test timeout, rate limit, provider 4xx/5xx, malformed JSON và circuit breaker fallback.
- [ ] Security test prompt injection trong tên/mô tả sản phẩm và output chứa product ID ngoài allowlist.
- [ ] Privacy test đảm bảo payload gửi provider không chứa email, phone, address, token hoặc order note.
- [ ] Load test recommendation endpoint và kiểm tra cache hit/miss.

### 17.14. Lộ trình triển khai RAG theo pha

| Pha | Phạm vi | Kết quả đầu ra | Điều kiện qua pha |
| --- | --- | --- | --- |
| `R0 — Baseline` | Món bán chạy + rule theo category/giá, chưa dùng vector/LLM | API recommendation có fallback và tracking | Có metric baseline, không ảnh hưởng luồng đặt hàng |
| `R1 — Semantic Retrieval` | Metadata sản phẩm, embedding, vector store, Top-N retrieval | Ứng viên semantic có filter active/giá | Recall/coverage đạt ngưỡng nội bộ; re-index ổn định |
| `R2 — Personal Ranking` | Preference profile và hybrid ranking | Gợi ý theo lịch sử mua | Vượt baseline offline; không rò rỉ dữ liệu cá nhân |
| `R3 — RAG Explanation` | LLM tạo lý do từ candidate allowlist | Lời giải thích có schema và fallback | 100% product ID hợp lệ; timeout không làm lỗi trang |
| `R4 — Experiment` | Feature flag, A/B test và dashboard metric | Báo cáo hiệu quả thực tế | Treatment không kém baseline và chi phí trong ngân sách |

### 17.15. Definition of Done cho module RAG

- [ ] Recommendation vẫn hoạt động ở chế độ baseline khi vector store hoặc LLM không khả dụng.
- [ ] Không có giá, sản phẩm hoặc coupon do LLM tự tạo.
- [ ] Tất cả món trả về được backend kiểm tra active/tồn kho/giá sau retrieval.
- [ ] Payload sang provider không chứa dữ liệu cá nhân hoặc secret.
- [ ] Có version cho embedding, profile và thuật toán; có thể rollback.
- [ ] Có metric offline/online và so sánh với baseline.
- [ ] Có unit, integration, security và fallback test.
- [ ] Người dùng có thể opt-out và xóa dữ liệu cá nhân hóa.
- [ ] Chi phí, latency và lỗi provider được theo dõi.
- [ ] Cập nhật sơ đồ kiến trúc, API docs, biến môi trường và hướng dẫn vận hành/re-index.

