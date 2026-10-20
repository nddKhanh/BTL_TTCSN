# Git workflow cho nhóm nhiều thành viên

## 1. Nguyên tắc danh tính và quyền truy cập

- Mỗi GitHub account thuộc một người thật, có email đã verify và được mời làm collaborator.
- Chủ tài khoản tự đăng nhập, tự chấp nhận lời mời và tự push nhánh của mình.
- Không gửi password, personal access token, recovery code hoặc SSH private key cho người khác hay cho trợ lý.
- Không dùng `git commit --author`, biến ngày commit hoặc commit rỗng để tạo lịch sử đóng góp giả.
- Nên dùng một clone riêng cho mỗi thành viên để tránh nhầm credential và `user.email`.
- Nếu cùng dùng một máy, chủ tài khoản phải trực tiếp xác nhận phiên đăng nhập trước khi push.

Trợ lý có thể chuẩn bị thay đổi, kiểm tra diff, đề xuất commit và hướng dẫn push.
Việc gán tác giả chỉ hợp lệ khi người đứng tên đã thực sự tham gia, đọc diff, chạy kiểm tra và chấp nhận nội dung.

## 2. Nhận việc

1. Điều phối viên tạo issue từ [mẫu công việc](templates/git-task-template.md).
2. Ghi một PR ID trong [work-packages.md](work-packages.md), assignee, reviewer và dependency.
3. Assignee xác nhận có thể giải thích mục tiêu, test và rủi ro.
4. Chỉ chuyển issue sang `In progress` khi dependency đã merge.

## 3. Chuẩn bị danh tính và repository

Kiểm tra, không in credential:

```powershell
git status --short
git branch --show-current
git config --get user.name
git config --get user.email
gh auth status
```

Nếu tên/email không thuộc người đang làm, dừng trước khi commit.
Chủ tài khoản tự cấu hình clone của mình bằng tên và email GitHub đã verify.

## 4. Tạo nhánh riêng từ main mới nhất

Working tree phải sạch trước khi đổi nhánh:

```powershell
git fetch origin --prune
git switch main
git pull --ff-only origin main
git switch -c feat/business-error-codes
```

Đổi tên nhánh theo work package.
Dùng tiền tố `feat/`, `fix/`, `test/`, `docs/`, `refactor/`, `chore/` hoặc `ci/`.

## 5. Làm việc và tạo commit nhỏ

Chỉ stage đúng file của một ý nghĩa:

```powershell
git status --short
git diff -- path/to/file1 path/to/file2
git add -- path/to/file1 path/to/file2
git diff --cached --name-only
(git diff --cached --name-only | Sort-Object -Unique).Count
```

Chỉ commit khi số file là 1–3 và staged diff tự đứng độc lập.

```powershell
git diff --cached --check
git commit -m "feat(order): require guest lookup proof"
```

Commit message dùng tiếng Anh và dạng:

```text
<type>(<scope>): <imperative summary>
```

Không chia commit theo số dòng hoặc tạo nhiều commit chỉ để đủ chỉ tiêu.
Nếu một thay đổi thật sự không thể chạy đúng với tối đa 3 file, cập nhật issue và tách thành hai lát cắt hoàn chỉnh trước khi tiếp tục.

## 6. Kiểm tra cục bộ

Khi chạm backend:

```powershell
Set-Location backend
.\mvnw.cmd test
Set-Location ..
```

Luôn chạy:

```powershell
git diff --check origin/main...HEAD
git status --short
```

Với frontend, ghi rõ trình duyệt, kích thước viewport và luồng đã kiểm tra trong PR.

## 7. Merge main để kiểm tra conflict mà không tạo commit lớn

Để giữ lịch sử chỉ gồm commit công việc tối đa 3 file, dùng dry-merge trước PR:

```powershell
git fetch origin
git merge --no-commit --no-ff origin/main
```

- Nếu Git báo `Already up to date`, chạy test và tiếp tục; không có merge để abort.
- Nếu merge chuẩn bị thành công, chạy test trên trạng thái đã kết hợp, sau đó `git merge --abort`.
- Nếu có conflict, ghi danh sách bằng `git diff --name-only --diff-filter=U`, sau đó `git merge --abort`.

Khi có conflict, làm mới nhánh từ `origin/main` và cherry-pick lần lượt các commit công việc vào nhánh mới:

```powershell
git branch backup/pr-id-before-refresh
git switch -c fix/pr-id-refreshed origin/main
git cherry-pick <commit-1-sha>
git cherry-pick <commit-2-sha>
```

Giải quyết conflict ngay trong từng cherry-pick, chạy lại test và kiểm tra mỗi commit vẫn không quá 3 file.
Không xóa nhánh backup cho đến khi PR đã merge.

## 8. Kiểm tra giới hạn PR

```powershell
git fetch origin
git log --oneline origin/main..HEAD
git diff --stat origin/main...HEAD
(git diff --name-only origin/main...HEAD | Sort-Object -Unique).Count
```

PR chỉ được mở khi có 1–19 file thay đổi.
Nếu có từ 20 file, tách theo hành vi/miền nghiệp vụ thành PR độc lập và cập nhật dependency.

## 9. Push nhánh

Chính chủ tài khoản thực hiện trong phiên đăng nhập của mình:

```powershell
git push -u origin HEAD
```

Không force-push sau khi đã có review, trừ khi reviewer đồng ý và lý do được ghi trong PR.
Không push trực tiếp lên `main`.

## 10. Tạo PR vào main

PR phải có:

- một mục tiêu rõ và link issue;
- danh sách hai commit có ý nghĩa;
- số file thay đổi;
- test/QA đã chạy và kết quả;
- ảnh/video nếu thay đổi UI;
- rủi ro, migration và rollback nếu có;
- reviewer khác tác giả.

Có thể dùng GitHub CLI sau khi chủ tài khoản đã đăng nhập; lệnh sẽ hỏi nội dung PR tương tác:

```powershell
gh pr create --base main --head (git branch --show-current) --title "feat(order): secure guest lookup"
```

Copy phần PR trong template vào trình soạn thảo hoặc điền trực tiếp trên GitHub.

## 11. Review và ghép vào main

1. Reviewer đọc từng commit và checkout nhánh nếu cần.
2. CI, test và file-count gate phải xanh.
3. Tác giả xử lý comment bằng commit nhỏ mới; vẫn tối đa 3 file/commit.
4. Reviewer approve và resolve conversation.
5. Chọn merge strategy thống nhất của nhóm; ưu tiên **Create a merge commit** để giữ các commit nguyên tử đã được chấm.
6. Sau merge, xóa nhánh remote và cập nhật bảng theo dõi.

Nếu trường yêu cầu nhìn thấy từng commit của từng thành viên, không squash PR.

## 12. Báo cáo cuối phiên

Mỗi phiên phải báo:

- người đang thao tác và PR ID;
- branch/HEAD;
- file đã thay đổi và số file;
- commit đã tạo hoặc đề xuất;
- test đã chạy và kết quả;
- trạng thái dry-merge/conflict;
- bước tiếp theo và ai chịu trách nhiệm.
