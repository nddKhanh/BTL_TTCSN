# Phase 2 — 15 PR / 30 commit bảo mật và tài khoản

## Quy ước

- Mỗi PR có đúng 2 commit dự kiến và được tính 3 điểm.
- Mỗi commit tối đa 3 file; mỗi PR mục tiêu 2–6 file và bắt buộc dưới 20 file.
- `A01`–`A30` là mã commit kế hoạch, không phải SHA Git.
- Migration và code phụ thuộc migration có thể ở hai commit khác nhau nhưng PR chỉ merge khi cả hai cùng đạt test.
- Nếu phạm vi thực tế vượt 3 file/commit, phải chia lát cắt và tái cân bằng trước khi stage.

## Backlog

| PR | Nhánh gợi ý | Commit 1 | Commit 2 | Phụ thuộc | Tiêu chí nghiệm thu chính |
| --- | --- | --- | --- | --- | --- |
| `SA01` | `feat/mail-foundation` | `A01 build(mail): add typed SMTP configuration` — `pom.xml`, config, `application.yml` | `A02 feat(mail): add gateway and fake provider` — interface, implementation, test | Không | Test/CI không gửi mail thật; secret chỉ từ env |
| `SA02` | `feat/account-status-tokens` | `A03 feat(auth): add account status fields` — migration, `AppUser`, status enum | `A04 feat(auth): persist one-time token hashes` — entity, repository, token purpose | Không | Giữ được user cũ; token plaintext không lưu DB |
| `SA03` | `feat/email-verification` | `A05 feat(auth): issue registration verification token` — service, event/DTO, controller | `A06 test(auth): cover email verification lifecycle` — tối đa 3 test file | `SA01`, `SA02` | TTL, one-time, resend và verified transition đúng |
| `SA04` | `feat/verification-ui` | `A07 feat(ui): add verify and resend flow` — tối đa 3 frontend file | `A08 test(ui): document verification states` — UI test/QA/docs tối đa 3 file | `SA03` | Success/expired/used/rate-limited state rõ; không lộ email |
| `SA05` | `feat/password-recovery` | `A09 feat(auth): add forgot and reset password APIs` — controller, service, DTO | `A10 test(auth): revoke sessions after password reset` — tối đa 3 test file | `SA01`, `SA02`, `SA08` | Response chung; token một lần; password mới thu hồi session |
| `SA06` | `feat/login-email-alerts` | `A11 feat(auth): publish successful login events` — event, auth service, listener | `A12 feat(mail): send deduplicated login alerts` — template/service/test tối đa 3 file | `SA01`, `SA08` | Login không chờ SMTP; mail có time/device/IP rút gọn; dedupe đúng |
| `SA07` | `fix/jwt-claims-policy` | `A13 fix(jwt): add issuer audience jti and token type` — properties, service, config | `A14 test(jwt): reject wrong claims and token type` — service/filter test tối đa 3 file | Không | `sub` là user ID; access 10–15 phút; claim sai bị 401 |
| `SA08` | `feat/refresh-sessions` | `A15 feat(auth): persist refresh sessions` — migration, entity, repository | `A16 feat(auth): issue access and rotating refresh tokens` — service, DTO, controller | `SA02`, `SA07` | Chỉ lưu refresh hash; rotation trong transaction; cookie policy rõ |
| `SA09` | `feat/session-revocation` | `A17 feat(auth): revoke current and all sessions` — service, controller, repository | `A18 feat(ui): refresh once and expose logout all` — API client, auth UI, test | `SA08` | Logout có hiệu lực server-side; không tạo vòng lặp refresh |
| `SA10` | `feat/rbac-permissions` | `A19 feat(authz): add role permission model` — migration, role, permission | `A20 feat(authz): load granted permissions` — user-details service, repository, test | `SA02` | Migrate CUSTOMER/ADMIN; role mới STAFF/MANAGER hoạt động |
| `SA11` | `test/authorization-matrix` | `A21 refactor(authz): enforce service method permissions` — tối đa 3 service/config file | `A22 test(authz): cover role permission matrix` — tối đa 3 test file | `SA10` | Có test 200/401/403; customer không nâng quyền; bảo vệ admin cuối |
| `SA12` | `feat/account-lockout` | `A23 feat(auth): track failures and temporary lockout` — user/service/repository | `A24 test(auth): cover lock expiry and safe errors` — error code + test tối đa 3 file | `SA02` | Counter theo account/IP; tự mở đúng hạn; lỗi không enumeration |
| `SA13` | `feat/auth-rate-limits` | `A25 feat(security): configure auth endpoint rate limits` — dependency/properties/filter | `A26 test(security): cover rate-limit keys and headers` — config/test tối đa 3 file | `SA03`, `SA05`, `SA12` | Login/resend/reset/refresh bị giới hạn; proxy header có trust policy |
| `SA14` | `feat/totp-mfa` | `A27 feat(mfa): persist encrypted TOTP enrollment` — migration/entity/service | `A28 feat(mfa): add challenge and recovery codes` — controller, DTO, test | `SA08`, `SA12` | Secret mã hóa; recovery code hash/one-time; clock skew có giới hạn |
| `SA15` | `feat/security-audit` | `A29 feat(audit): persist security events` — migration, entity, repository/listener | `A30 feat(admin): query sanitized security audit` — service, controller, test/UI tối đa 3 file | `SA06`, `SA09`, `SA10`, `SA12` | Không log secret; filter theo event/result/time; chỉ REPORT_READ |

## Thứ tự wave

```text
Wave A: SA01 SA02 SA07
          |    |    |
Wave B: SA03 SA10 SA12 SA08
          |    |     \  |
Wave C: SA04 SA11 SA13 SA06 SA09
               \      |   /
Wave D:          SA05 SA14 SA15
```

- `SA05` cần session revocation nên chỉ code sau khi `SA08` merge.
- `SA06` gửi thông báo sau khi session được tạo thành công.
- `SA14` là MFA TOTP; không dùng email như yếu tố thứ hai duy nhất.
- `SA15` chỉ lưu dữ liệu đã rút gọn/sanitized và có retention policy.

## Cổng release

- `A0/A1`: đăng ký → verify → login → forgot/reset chạy được bằng fake mail.
- `A2`: access hết hạn có thể refresh đúng một lần; token reuse thu hồi family.
- `A3`: toàn bộ ma trận role/permission có controller hoặc method-security test.
- `A4`: login email, lockout, rate limit và audit không làm lộ email/token.
- `A5`: MFA setup/challenge/recovery đều có happy path và abuse cases.
