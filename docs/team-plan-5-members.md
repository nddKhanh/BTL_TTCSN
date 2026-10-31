# Phương án chia việc cho 5 thành viên

> Đây là phương án thay thế cho kế hoạch 3 thành viên, không chạy song song.
> Điền GitHub username sau khi từng chủ tài khoản đã tham gia repository.

## 1. Thành viên và nguyên tắc công bằng

| Mã | Họ tên | GitHub username | Mảng chính | Người review mặc định |
| --- | --- | --- | --- | --- |
| `M1` | Chuyên | `@pending` | API contract, OpenAPI, CI | `M2` — Tiến |
| `M2` | Tiến | `@pending` | Security, test nghiệp vụ, dashboard | `M3` — Cường |
| `M3` | Cường | `@pending` | Order lookup, catalog backend, checkout | `M4` — Bách |
| `M4` | Bách | `@pending` | Catalog UI, cart, accessibility | `M5` — Khánh |
| `M5` | Khánh | `@pending` | Admin, Docker, tài liệu/QA | `M1` — Chuyên |

Trong mỗi phase, mỗi người nhận đúng:

- 3 PR;
- 6 commit công việc;
- 9 điểm kế hoạch;
- ít nhất 2 PR review cho người khác.

Sau cả hai phase, mỗi người có 6 PR, 12 commit và 18 điểm kế hoạch.

Người đứng tên phải có đóng góp thật, tự chạy kiểm tra, đọc diff và giải thích được phần mình.
Không tạo commit rỗng hoặc đổi tác giả chỉ để làm đều biểu đồ đóng góp.

## 2. Phân công

| Thành viên | PR được giao | Commit | Điểm | Thứ tự đề xuất |
| --- | --- | ---: | ---: | --- |
| `M1` — Chuyên | `PR01`, `PR05`, `PR13` | 6 | 9 | PR01 → PR05 → PR13 |
| `M2` — Tiến | `PR02`, `PR04`, `PR11` | 6 | 9 | PR02 → PR04 → PR11 |
| `M3` — Cường | `PR03`, `PR06`, `PR09` | 6 | 9 | PR06 → PR03 → PR09 |
| `M4` — Bách | `PR07`, `PR08`, `PR14` | 6 | 9 | PR08 → PR07 → PR14 |
| `M5` — Khánh | `PR10`, `PR12`, `PR15` | 6 | 9 | PR12 → PR10 → PR15 |

Chi tiết mục tiêu, dependency và commit message nằm trong [work-packages.md](work-packages.md).

### Phase 2 — Identity, security và email

| Thành viên | PR được giao | Commit | Điểm | Thứ tự đề xuất |
| --- | --- | ---: | ---: | --- |
| `M1` — Chuyên | `SA01`, `SA07`, `SA13` | 6 | 9 | SA01 → SA07 → SA13 |
| `M2` — Tiến | `SA02`, `SA08`, `SA12` | 6 | 9 | SA02 → SA12 → SA08 |
| `M3` — Cường | `SA03`, `SA05`, `SA09` | 6 | 9 | SA03 → SA09 → SA05 |
| `M4` — Bách | `SA04`, `SA06`, `SA14` | 6 | 9 | SA04 → SA06 → SA14 |
| `M5` — Khánh | `SA10`, `SA11`, `SA15` | 6 | 9 | SA10 → SA11 → SA15 |

Chi tiết phase 2 nằm trong [security-auth-work-packages.md](security-auth-work-packages.md).

## 3. Lịch wave

### Phase 1

| Wave | M1 — Chuyên | M2 — Tiến | M3 — Cường | M4 — Bách | M5 — Khánh |
| --- | --- | --- | --- | --- | --- |
| 1 | PR01 | PR02, PR04 | PR06 | PR08 | PR12 |
| 2 | PR05 | PR11 | PR03 | PR07 | PR10 |
| 3 | PR13 | Review/QA chéo | PR09 | PR14 | PR15 |

### Phase 2

| Wave | M1 — Chuyên | M2 — Tiến | M3 — Cường | M4 — Bách | M5 — Khánh |
| --- | --- | --- | --- | --- | --- |
| A | SA01, SA07 | SA02 | Chuẩn bị SA03 | Review mail/JWT | Chuẩn bị SA10 |
| B | Chuẩn bị SA13 | SA12, SA08 | SA03 | Chờ SA03/SA08 | SA10 |
| C | SA13 sau dependency | Review session | SA09, SA05 | SA04, SA06 | SA11 |
| D | Review security gate | Review MFA/audit | Review recovery | SA14 | SA15 |

Mỗi người chỉ code một PR tại một thời điểm.
Các ô review/QA là công việc thật nhưng không tạo commit nếu không có thay đổi cần thiết.
Nếu phải tách thêm commit, điều phối viên cập nhật backlog trước khi code và tái phân công để mỗi người vẫn có tổng commit bằng nhau.

## 4. Quy trình bắt buộc cho từng PR

1. **Nhận việc:** assignee nhận issue chứa PR ID, hai commit dự kiến, file dự kiến, acceptance criteria và reviewer.
2. **Xác nhận danh tính:** chủ tài khoản tự đăng nhập GitHub; kiểm tra `git config user.name` và `git config user.email` thuộc chính mình.
3. **Tạo nhánh:** fetch, chuyển về `main`, fast-forward rồi tạo nhánh đúng tên trong bảng work package.
4. **Thực hiện:** chỉ sửa phạm vi issue; stage theo đường dẫn cụ thể, không dùng `git add .`.
5. **Commit nhỏ:** trước mỗi commit đếm file đã stage; chỉ commit khi có 1–3 file và một mục đích kiểm thử được.
6. **Kiểm tra:** chạy test liên quan, test toàn backend khi chạm Java và `git diff --check`.
7. **Kiểm tra conflict với main:** fetch rồi dry-merge `origin/main`; giải quyết bằng nhánh làm mới nếu có conflict, không tạo commit tích hợp lớn.
8. **Push:** chính chủ tài khoản push nhánh của mình lên `origin`; không chia sẻ token/mật khẩu.
9. **Tạo PR:** mở PR từ nhánh cá nhân vào `main`, dưới 20 file, điền test evidence và gắn reviewer.
10. **Ghép main:** chỉ merge khi CI xanh, reviewer approve và conversation đã resolve; xóa nhánh remote sau merge.

Lệnh chi tiết và cách xử lý conflict xem [git-workflow.md](git-workflow.md).

## 5. Bảng theo dõi phase 1

| PR | Assignee | Issue | Branch | 2 commit đủ nghĩa | File/commit ≤ 3 | File/PR ≤ 19 | Dry-merge main | Test/CI | Review | Merged |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PR01 | M1 — Chuyên |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 — Tiến | ☐ |
| PR02 | M2 — Tiến |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 — Cường | ☐ |
| PR03 | M3 — Cường |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M4 — Bách | ☐ |
| PR04 | M2 — Tiến |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 — Cường | ☐ |
| PR05 | M1 — Chuyên |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 — Tiến | ☐ |
| PR06 | M3 — Cường |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M4 — Bách | ☐ |
| PR07 | M4 — Bách |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M5 — Khánh | ☐ |
| PR08 | M4 — Bách |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M5 — Khánh | ☐ |
| PR09 | M3 — Cường |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M4 — Bách | ☐ |
| PR10 | M5 — Khánh |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 — Chuyên | ☐ |
| PR11 | M2 — Tiến |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 — Cường | ☐ |
| PR12 | M5 — Khánh |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 — Chuyên | ☐ |
| PR13 | M1 — Chuyên |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 — Tiến | ☐ |
| PR14 | M4 — Bách |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M5 — Khánh | ☐ |
| PR15 | M5 — Khánh |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 — Chuyên | ☐ |

Phase 2 dùng cùng cột theo dõi; tạo các dòng `SA01`–`SA15` từ bảng phân công trước khi bắt đầu wave A.
