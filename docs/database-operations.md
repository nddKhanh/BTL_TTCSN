# Sao lưu và khôi phục MySQL

Các lệnh dưới đây dùng MySQL client trên Windows và giả định database local có tên
`coffee_order_db`. Thay host, port, user hoặc tên database nếu `DB_URL` của môi trường khác.

## Sao lưu trước migration

Tạo thư mục `backups` ở root dự án. Thư mục này đã được `.gitignore` để tránh commit dữ liệu.
Chạy lệnh sau từ root dự án; tùy chọn `--password` sẽ yêu cầu nhập mật khẩu thay vì lưu mật
khẩu trong command history:

```powershell
New-Item -ItemType Directory -Force -Path .\backups
mysqldump --host=localhost --port=3306 --user=root --password --single-transaction --routines --triggers --result-file=.\backups\coffee_order_db.sql coffee_order_db
```

Kiểm tra file backup tồn tại và có dung lượng lớn hơn `0` trước khi chạy backend với phiên bản
migration mới:

```powershell
Get-Item .\backups\coffee_order_db.sql | Select-Object FullName, Length, LastWriteTime
```

## Khôi phục

Khôi phục nên thực hiện vào một database mới để tránh trộn dữ liệu cũ và dữ liệu backup.
Nếu tên ví dụ bên dưới đã tồn tại, hãy chọn tên mới thay vì xóa database đó:

```powershell
mysql --host=localhost --port=3306 --user=root --password -e "CREATE DATABASE coffee_order_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql --host=localhost --port=3306 --user=root --password coffee_order_restore
```

Trong MySQL prompt, nạp file bằng đường dẫn tuyệt đối dùng dấu `/`:

```sql
SOURCE D:/University/5/TTCSN/backups/coffee_order_db.sql;
```

Sau khi kiểm tra dữ liệu trong `coffee_order_restore`, trỏ `DB_URL` tạm thời tới database này
để xác nhận ứng dụng khởi động và Flyway/JPA validate thành công. Không khôi phục đè production
khi chưa có cửa sổ bảo trì và bản sao lưu đã được kiểm tra.

## Quy tắc môi trường

| Profile | Hibernate `ddl-auto` | Flyway baseline | Seed demo |
| --- | --- | --- | --- |
| mặc định | `validate` | tắt | không |
| `dev` | `update` | version `0` khi cần | có |
| `demo` | `update` | version `0` khi cần | có |
| `test` | `create-drop` | tắt | không |
| `prod` | `validate` | tắt | không |

Production không tự baseline một schema không có lịch sử Flyway. Nếu tiếp quản database cũ,
hãy sao lưu, kiểm tra schema, chạy migration trên bản khôi phục trước rồi mới baseline có chủ đích.
