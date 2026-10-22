# Phase 1 — 15 PR / 30 commit ổn định và sản phẩm

Phase 2 cho email, JWT session, RBAC, MFA và audit nằm tại [security-auth-work-packages.md](security-auth-work-packages.md).

## Quy ước

- Mỗi hàng là một PR gồm đúng 2 commit dự kiến.
- Mỗi commit chỉ được chạm tối đa 3 file; toàn PR tối đa 19 file, mục tiêu thực tế là 2–6 file.
- Mỗi PR được tính 3 điểm công việc, gồm code, test/QA và mô tả PR.
- Tên file là phạm vi dự kiến, không phải quyền sửa mọi file khớp glob.
- Nếu phát hiện cần hơn 3 file trong một commit, dừng trước khi stage, tách việc và cập nhật lại bảng phân công để số commit giữa các thành viên vẫn cân bằng.

## Backlog đã chuẩn hóa

| PR | Nhánh gợi ý | Commit 1 | Commit 2 | Phụ thuộc | Tiêu chí nghiệm thu chính |
| --- | --- | --- | --- | --- | --- |
| `PR01` | `feat/business-error-codes` | `C01 feat(api): add domain error codes` — tối đa 3 file exception | `C02 test(api): cover stable domain errors` — tối đa 2 file test | Không | Order/coupon/auth trả mã ổn định; test xanh |
| `PR02` | `test/security-route-matrix` | `C03 refactor(security): group route authorization rules` — tối đa 2 file | `C04 test(security): verify public customer admin routes` — tối đa 2 file | Không | Public/customer/admin đúng ma trận; 401/403 đúng contract |
| `PR03` | `fix/secure-order-lookup` | `C05 fix(order): require guest lookup proof` — tối đa 3 file backend | `C06 fix(ui): send phone or lookup token for order lookup` — tối đa 3 file frontend/test | `PR01`, `PR02` | Biết mã đơn thôi không đủ xem đơn; owner vẫn xem được |
| `PR04` | `test/order-pricing-coupons` | `C07 test(order): cover shipping and option pricing` — 1–2 file test | `C08 test(coupon): cover validity and discount boundaries` — 1–2 file test | `PR01` | Có test biên miễn phí ship, percent/fixed, hết hạn, minimum order |
| `PR05` | `docs/openapi-core-flows` | `C09 docs(api): describe auth and order OpenAPI` — tối đa 3 file | `C10 docs(api): add request response examples` — tối đa 3 file | `PR01` | Swagger thể hiện JWT, status và ví dụ lỗi chính |
| `PR06` | `feat/catalog-page-sort` | `C11 feat(catalog): add page and sort query` — tối đa 3 file backend | `C12 test(catalog): cover page sort boundaries` — tối đa 3 file test/controller | Không | Sort allowlist; page có metadata; input sai trả 400 |
| `PR07` | `feat/catalog-url-state` | `C13 feat(ui): persist catalog filters in URL` — tối đa 2 file | `C14 feat(ui): add loading empty retry states` — tối đa 3 file | `PR06` | Refresh/back giữ filter; API lỗi không làm trang trống vô nghĩa |
| `PR08` | `feat/cart-edit-options` | `C15 feat(cart): merge identical cart lines` — tối đa 2 file | `C16 feat(cart): edit options and show shipping progress` — tối đa 3 file | Không | Khóa dòng gồm product/options/note; tổng tiền local đúng |
| `PR09` | `fix/checkout-validation` | `C17 fix(order): validate Vietnamese delivery input` — tối đa 3 file backend | `C18 fix(checkout): mirror validation and messages` — tối đa 3 file frontend/test | `PR01` | Frontend hỗ trợ người dùng; backend là nguồn quyết định cuối |
| `PR10` | `feat/admin-pagination` | `C19 feat(admin): page order and product queries` — tối đa 3 file backend | `C20 feat(admin-ui): navigate paged tables` — tối đa 3 file frontend | `PR06` | Filter giữ nguyên khi đổi trang; không tải toàn bộ dữ liệu |
| `PR11` | `feat/dashboard-date-range` | `C21 feat(dashboard): aggregate metrics by date range` — tối đa 3 file backend | `C22 test(dashboard): verify completed revenue metrics` — tối đa 3 file | Không | Chỉ tính `COMPLETED`; biên ngày được test; không tính trong bộ nhớ |
| `PR12` | `chore/local-containers` | `C23 build(docker): add non-root backend image` — tối đa 2 file | `C24 chore(docker): add local compose and run guide` — tối đa 3 file | Không | Build image được; healthcheck; không đóng gói secret |
| `PR13` | `ci/pull-request-gates` | `C25 ci(test): run Maven tests on pull requests` — tối đa 2 file | `C26 ci(security): add dependency and secret checks` — tối đa 3 file | `PR04` | CI fail khi test/secret check fail; cache không chứa secret |
| `PR14` | `fix/frontend-accessibility` | `C27 fix(a11y): add dialog focus and keyboard behavior` — tối đa 3 file | `C28 fix(a11y): improve labels contrast and status semantics` — tối đa 3 file | `PR07`, `PR08` | Tab/Escape/focus hợp lý; label/status đọc được bằng screen reader |
| `PR15` | `docs/defense-package` | `C29 docs(architecture): add ERD and system diagrams` — tối đa 3 file docs | `C30 docs(qa): add demo and acceptance evidence` — tối đa 3 file docs | Các PR cần demo | Người mới dựng/chạy/demo được; sơ đồ khớp code/schema hiện tại |

## Thứ tự wave

```text
Wave 1: PR01 PR02 PR04 PR06 PR08 PR11 PR12
             |    |         |         |
Wave 2:     PR03 PR05       PR07      PR10 PR13
                       \      |       /
Wave 3:                PR09  PR14   PR15
```

- Chỉ nhận PR ở wave sau khi dependency đã merge vào `main`.
- Một thành viên chỉ giữ tối đa một PR đang code và một PR đang review.
- Không giữ nhánh dài ngày; mục tiêu hoàn thành một PR trong 1–2 phiên làm việc.

## Cổng kiểm soát số file

Trước mỗi commit:

```powershell
git diff --cached --name-only
(git diff --cached --name-only | Sort-Object -Unique).Count
```

Kết quả phải từ 1 đến 3.

Trước khi mở PR, so với `origin/main`:

```powershell
git fetch origin
(git diff --name-only origin/main...HEAD | Sort-Object -Unique).Count
```

Kết quả phải từ 1 đến 19.
