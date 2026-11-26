# Lộ trình mở rộng xác thực, phân quyền, JWT và email

## 1. Hiện trạng và khoảng trống

Code hiện tại đã có BCrypt cost 12, access JWT ký HMAC, filter xác thực và hai role `CUSTOMER`/`ADMIN`.
Các khoảng trống cần xử lý theo thứ tự:

- Access token sống 120 phút, dùng email làm `sub`, chưa có `issuer`, `audience`, `jti` hoặc `token_type`.
- Chưa có refresh token, rotation, reuse detection, logout phía server hoặc thu hồi toàn bộ phiên.
- Chưa xác minh email, quên/đặt lại mật khẩu hoặc trạng thái tài khoản.
- Chưa có mail provider, hàng đợi/retry, template và chế độ test không gửi email thật.
- Phân quyền mới dừng ở hai role; chưa có permission chi tiết và method-level authorization đầy đủ.
- Chưa có rate limit, lockout, MFA, quản lý thiết bị/phiên và audit log bảo mật.
- Frontend đang lưu JWT ở `localStorage`, làm tăng tác động nếu có XSS.

## 2. Nguyên tắc kiến trúc

- Access JWT sống ngắn, dự kiến 10–15 phút; refresh token là chuỗi ngẫu nhiên opaque và chỉ lưu bản hash trong database.
- JWT dùng user ID bất biến làm `sub`; email chỉ là dữ liệu hồ sơ, không phải định danh token lâu dài.
- Claim bắt buộc: `iss`, `aud`, `sub`, `iat`, `exp`, `jti`, `token_type`; role/permission chỉ chứa dữ liệu cần cho authorization.
- Refresh token rotation sau mỗi lần dùng; phát hiện reuse thì thu hồi toàn bộ token family/session liên quan.
- Refresh token trên web ưu tiên cookie `HttpOnly`, `Secure`, `SameSite`; access token giữ trong memory khi frontend được nâng cấp.
- Token xác minh email, reset mật khẩu và recovery chỉ lưu hash, dùng một lần, có TTL và bị vô hiệu sau khi sử dụng.
- Email được phát qua abstraction; lỗi SMTP không làm login thất bại. Có retry giới hạn, dead-letter/audit và không log nội dung nhạy cảm.
- Authorization kiểm tra ở cả route và service method; controller không tự quyết quyền nghiệp vụ.
- Thông báo lỗi đăng nhập/reset dùng nội dung chung để tránh dò email tồn tại.
- Password, JWT, refresh token, OTP, link token và secret không xuất hiện trong log hoặc response không cần thiết.

## 3. Mô hình tài khoản và quyền

### Trạng thái tài khoản

```text
PENDING_VERIFICATION -> ACTIVE -> LOCKED
                           |         |
                           +-------> DISABLED
```

- `PENDING_VERIFICATION`: đăng ký xong nhưng chưa xác minh email.
- `ACTIVE`: được đăng nhập và sử dụng hệ thống.
- `LOCKED`: khóa tạm do đăng nhập sai/rủi ro; có `lockedUntil`.
- `DISABLED`: admin vô hiệu hóa hoặc người dùng yêu cầu đóng tài khoản.

### Role và permission đề xuất

| Role | Permission chính |
| --- | --- |
| `CUSTOMER` | Xem/sửa hồ sơ của mình, đặt và xem đơn của mình |
| `STAFF` | Xem đơn, xác nhận/giao đơn; không quản lý user hoặc cấu hình hệ thống |
| `MANAGER` | Quản lý catalog, coupon, báo cáo và staff trong phạm vi cho phép |
| `ADMIN` | Quản trị hệ thống; không được xóa/hạ quyền admin cuối cùng |

Permission dự kiến: `ORDER_READ`, `ORDER_UPDATE`, `CATALOG_WRITE`, `COUPON_WRITE`, `REPORT_READ`, `USER_READ`, `USER_STATUS_WRITE`, `ROLE_ASSIGN`.

## 4. Email và thông báo đăng nhập

### Email bắt buộc

- Xác minh email sau đăng ký.
- Yêu cầu đặt lại mật khẩu.
- Mật khẩu đã thay đổi.
- Đăng nhập thành công.
- Phát hiện thiết bị mới hoặc refresh-token reuse.
- Tài khoản bị khóa/mở khóa hoặc role bị thay đổi.

### Email khi đăng nhập

Sau khi xác thực thành công và tạo session:

1. Ghi `LOGIN_SUCCESS` vào audit log.
2. Phát `LoginSucceededEvent` sau khi transaction thành công.
3. Đưa email vào outbox/worker; response đăng nhập không chờ SMTP.
4. Email chỉ chứa thời gian theo múi giờ người dùng, IP đã che một phần, trình duyệt/thiết bị rút gọn và nút “Không phải tôi”.
5. Link “Không phải tôi” yêu cầu xác nhận an toàn rồi thu hồi session, không chứa JWT access trong URL.
6. Chống spam bằng dedupe cùng user + device fingerprint trong cửa sổ cấu hình, ví dụ 10 phút.

Mặc định demo gửi mọi lần đăng nhập thành công.
Production có thể cho chọn `EVERY_LOGIN` hoặc `NEW_DEVICE_ONLY`, nhưng cảnh báo rủi ro cao không được tắt.

### Hạ tầng mail

- Local/test dùng fake gateway hoặc Mailpit; CI không gọi SMTP thật.
- Production dùng SMTP/provider qua biến môi trường.
- Template HTML có bản text fallback, không nhúng secret và escape dữ liệu người dùng.
- Ghi trạng thái `PENDING`, `SENT`, `FAILED`, số lần retry và lỗi đã làm sạch.

## 5. API dự kiến

| Method | Endpoint | Quyền | Mục đích |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Public | Tạo tài khoản pending và gửi email xác minh |
| `POST` | `/api/v1/auth/verify-email` | Public | Xác minh token một lần |
| `POST` | `/api/v1/auth/resend-verification` | Public, rate-limited | Gửi lại nếu hợp lệ; response không lộ email tồn tại |
| `POST` | `/api/v1/auth/login` | Public, rate-limited | Tạo access token và refresh session |
| `POST` | `/api/v1/auth/refresh` | Refresh cookie/token | Rotate refresh token và cấp access token mới |
| `POST` | `/api/v1/auth/logout` | Authenticated | Thu hồi session hiện tại |
| `POST` | `/api/v1/auth/logout-all` | Authenticated | Thu hồi mọi session của user |
| `POST` | `/api/v1/auth/forgot-password` | Public, rate-limited | Tạo reset request với response chung |
| `POST` | `/api/v1/auth/reset-password` | Public | Dùng token một lần và thu hồi mọi session |
| `POST` | `/api/v1/auth/mfa/setup` | Authenticated | Bắt đầu cấu hình TOTP |
| `POST` | `/api/v1/auth/mfa/confirm` | Authenticated | Xác nhận TOTP và phát recovery codes |
| `POST` | `/api/v1/auth/mfa/challenge` | Pre-auth | Hoàn tất đăng nhập có MFA |
| `GET` | `/api/v1/users/me/sessions` | Authenticated | Danh sách session/thiết bị đã rút gọn |
| `DELETE` | `/api/v1/users/me/sessions/{id}` | Owner | Thu hồi một session |
| `GET` | `/api/v1/admin/security/audit-events` | `REPORT_READ` | Tra cứu sự kiện bảo mật |

## 6. Dữ liệu và migration

- `users`: thêm `status`, `email_verified_at`, `failed_login_count`, `locked_until`, `password_changed_at`, `mfa_enabled` và version optimistic locking.
- `auth_tokens`: token hash, purpose, user, expiry, used/revoked time; dùng cho verify/reset ngắn hạn.
- `user_sessions`: refresh hash, family ID, device/IP rút gọn, created/last-used/expires/revoked time.
- `roles`, `permissions`, `role_permissions`, `user_roles`: chuyển từ enum đơn sang RBAC có migration tương thích dữ liệu hiện có.
- `mfa_secrets`: secret được mã hóa bằng key ngoài database; recovery code chỉ lưu hash.
- `security_audit_events`: event type, actor/target, result, IP rút gọn, user-agent rút gọn, request ID và timestamp.
- `mail_outbox`: template, recipient, payload không nhạy cảm, trạng thái, retry count và thời điểm gửi.

Migration phải giữ được user `CUSTOMER`/`ADMIN` hiện có và có rollback/backup guide.

## 7. Các pha triển khai

| Pha | Phạm vi | Điều kiện hoàn thành |
| --- | --- | --- |
| `A0 Mail foundation` | Mail gateway, cấu hình, template, fake provider, retry/outbox | Test không gửi mail thật; lỗi SMTP không làm hỏng request |
| `A1 Account lifecycle` | Trạng thái user, verify email, resend, forgot/reset password | Token hash/TTL/one-time; response chống enumeration |
| `A2 JWT sessions` | Claim chuẩn, access ngắn hạn, refresh rotation, logout/revoke, device sessions | Reuse detection và revoke-all test đạt |
| `A3 Authorization` | `STAFF`/`MANAGER`, permission, route + method security | Ma trận quyền có test 200/401/403; bảo vệ admin cuối cùng |
| `A4 Detection` | Login email, new-device alert, rate limit, lockout, audit log | Không spam; không log secret; admin filter audit được |
| `A5 MFA` | TOTP, recovery code, step-up cho thao tác nhạy cảm | Secret được mã hóa; recovery code một lần; clock-skew test |
| `A6 Frontend hardening` | Refresh cookie, token memory, session UI, verify/reset/MFA screens | Không còn access token dài hạn trong localStorage |

## 8. Kiểm thử bắt buộc

- Unit test TTL, hash token, rotation, reuse detection, lockout và permission mapping.
- MockMvc test cho register/verify/login/refresh/logout/reset/MFA và ma trận role.
- Integration test migration, unique constraint, transaction và concurrent refresh.
- Mail test bằng fake inbox: template đúng, không lộ token trong log, retry đúng giới hạn.
- Security test enumeration, brute force, revoked token, expired token, wrong issuer/audience/type và privilege escalation.
- Frontend test refresh một lần khi 401; tránh vòng lặp refresh vô hạn.
- Test đổi password/disable/role change làm session cũ mất hiệu lực theo chính sách.

## 9. Biến môi trường dự kiến

```dotenv
JWT_SECRET=<base64-secret>
JWT_ISSUER=moc-nhien-api
JWT_AUDIENCE=moc-nhien-web
JWT_ACCESS_MINUTES=15
REFRESH_TOKEN_DAYS=14
AUTH_TOKEN_PEPPER=<separate-secret>
MFA_ENCRYPTION_KEY=<separate-key>
MAIL_ENABLED=false
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@mocnhien.local
APP_PUBLIC_URL=http://localhost:5500
```

Không commit giá trị thật.
Production bắt buộc HTTPS trước khi bật `Secure` cookie và email chứa link public.

## 10. Gói công việc

Chi tiết 15 PR / 30 commit của phase này nằm trong [security-auth-work-packages.md](security-auth-work-packages.md).

## 11. Backlog bảo mật sau phase 2

- OAuth2/OIDC với Google; chỉ link tài khoản sau bước xác nhận rõ ràng, không tự gộp chỉ vì email trùng.
- Passkey/WebAuthn để giảm phụ thuộc mật khẩu và TOTP.
- Risk-based authentication theo thiết bị mới, quốc gia bất thường, tốc độ thay đổi IP và hành vi refresh.
- Step-up authentication trước khi đổi email, đổi role, xem dữ liệu nhạy cảm hoặc thu hồi toàn bộ session.
- Email xác nhận đơn, thay đổi trạng thái và hủy đơn sử dụng chung mail outbox; email marketing phải có consent/unsubscribe riêng.
- Trang notification preferences cho login alert, order alert và nội dung tùy chọn; cảnh báo bảo mật nghiêm trọng không bị tắt.
- Quy trình recovery cho admin cuối cùng, rotation signing key và sự cố token bị lộ.
- Dashboard metric cho login failure, lockout, refresh reuse, mail failure và privilege change; cảnh báo không chứa dữ liệu cá nhân thô.
