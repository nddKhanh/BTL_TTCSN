# Mẫu issue, commit, PR và báo cáo phiên

## A. Issue nhận việc

```markdown
## Work package

- PR ID:
- Mục tiêu:
- Assignee:
- Reviewer:
- Dependency:
- Branch:

## Hai commit dự kiến

1. `<type>(<scope>): <summary>`
   - File dự kiến (1–3):
   - Kiểm tra:
2. `<type>(<scope>): <summary>`
   - File dự kiến (1–3):
   - Kiểm tra:

## Acceptance criteria

- [ ]
- [ ]
- [ ]

## Rủi ro / ngoài phạm vi

- Rủi ro:
- Không làm trong PR này:
```
## B. Checklist trước commit

```markdown
- [ ] Tôi là người thực sự tham gia thay đổi và danh tính Git thuộc về tôi.
- [ ] Staged diff chỉ có một mục đích.
- [ ] Có từ 1 đến 3 file staged.
- [ ] Không có secret, file sinh tự động hoặc thay đổi ngoài phạm vi.
- [ ] `git diff --cached --check` đạt.
- [ ] Test nhỏ nhất liên quan đã đạt.
```

## C. Nội dung Pull Request

```markdown
## Mục tiêu

Closes #<issue>

## Thay đổi

-
-

## Commits

- `<sha> <message>`
- `<sha> <message>`

## Phạm vi

- Số file thay đổi: `<1-19>`
- Ngoài phạm vi:

## Kiểm tra

- [ ] Test liên quan:
- [ ] Full backend test nếu áp dụng:
- [ ] `git diff --check`:
- [ ] Dry-merge `origin/main`:
- [ ] QA trình duyệt/viewport nếu áp dụng:

## Bằng chứng UI/API

<!-- Ảnh, video, curl hoặc response mẫu; che token và dữ liệu cá nhân. -->

## Rủi ro và rollback

- Rủi ro:
- Migration:
- Cách rollback:

## Review

- Reviewer:
- [ ] CI xanh
- [ ] Conversation đã resolve
```

## D. Báo cáo cuối phiên

```markdown
## Session handoff

- Thành viên / GitHub:
- PR ID / Issue:
- Branch / HEAD:
- Trạng thái: `planned | coding | ready-to-commit | pushed | in-review | merged | blocked`
- File đã đổi: `<n>`
- Commit đã có:
- Test và kết quả:
- Dry-merge/conflict:
- Việc tiếp theo:
- Người chịu trách nhiệm tiếp theo:
```
