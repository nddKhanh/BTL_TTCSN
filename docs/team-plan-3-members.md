# Phương án chia việc cho 3 thành viên

> Đây là phương án thay thế cho kế hoạch 5 thành viên, không chạy song song.
> Điền GitHub username sau khi từng chủ tài khoản đã tham gia repository.

## 1. Thành viên và nguyên tắc công bằng

| Mã | GitHub username | Mảng chính | Người review mặc định |
| --- | --- | --- | --- |
| `M1` | `@pending` | API contract, test, OpenAPI, dashboard, CI | `M2` |
| `M2` | `@pending` | Security, order lookup, checkout, Docker, QA | `M3` |
| `M3` | `@pending` | Catalog, storefront, cart, admin UI, accessibility | `M1` |

Mỗi người nhận đúng:

- 5 PR;
- 10 commit công việc;
- 15 điểm kế hoạch;
- ít nhất 2 PR review cho người khác.

Số commit bằng nhau không thay thế cho đóng góp thật.
Người đứng tên phải trực tiếp làm hoặc pair-program phần việc, chạy kiểm tra, đọc diff và giải thích được PR.

## 2. Phân công

| Thành viên | PR được giao | Commit | Điểm | Thứ tự đề xuất |
| --- | --- | ---: | ---: | --- |
| `M1` | `PR01`, `PR04`, `PR05`, `PR11`, `PR13` | 10 | 15 | PR01 → PR04 → PR11 → PR05 → PR13 |
| `M2` | `PR02`, `PR03`, `PR09`, `PR12`, `PR15` | 10 | 15 | PR02 → PR12 → PR03 → PR09 → PR15 |
| `M3` | `PR06`, `PR07`, `PR08`, `PR10`, `PR14` | 10 | 15 | PR06 → PR08 → PR07 → PR10 → PR14 |

Chi tiết mục tiêu, dependency và commit message nằm trong [work-packages.md](work-packages.md).

## 3. Lịch wave

| Wave | M1 | M2 | M3 | Điều kiện đóng wave |
| --- | --- | --- | --- | --- |
| 1 | PR01, PR04 | PR02, PR12 | PR06, PR08 | Tất cả test liên quan xanh; dependency wave 2 đã sẵn sàng |
| 2 | PR11, PR05 | PR03 | PR07, PR10 | Main không có conflict; smoke test catalog/order/admin đạt |
| 3 | PR13 | PR09, PR15 | PR14 | CI xanh; QA và tài liệu bảo vệ có bằng chứng |

Không mở hai PR có cùng dependency chưa merge.
Nếu một PR phát sinh thêm commit, điều phối viên phải chuyển một commit tương đương ở wave chưa bắt đầu sang thành viên khác để giữ tổng số cân bằng.

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

## 5. Bảng theo dõi

| PR | Assignee | Issue | Branch | 2 commit đủ nghĩa | File/commit ≤ 3 | File/PR ≤ 19 | Dry-merge main | Test/CI | Review | Merged |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PR01 | M1 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 | ☐ |
| PR02 | M2 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 | ☐ |
| PR03 | M2 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 | ☐ |
| PR04 | M1 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 | ☐ |
| PR05 | M1 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 | ☐ |
| PR06 | M3 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 | ☐ |
| PR07 | M3 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 | ☐ |
| PR08 | M3 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 | ☐ |
| PR09 | M2 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 | ☐ |
| PR10 | M3 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 | ☐ |
| PR11 | M1 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 | ☐ |
| PR12 | M2 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 | ☐ |
| PR13 | M1 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M2 | ☐ |
| PR14 | M3 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M1 | ☐ |
| PR15 | M2 |  |  | ☐ | ☐ | ☐ | ☐ | ☐ | M3 | ☐ |
