# Backend integration tests

Toàn bộ test nằm trong `dotani-monolith/src/test` và chạy qua HTTP (MockMvc) với **JWT thật** và **MySQL 8.4 thật**, nên kiểm tra được cả filter bảo mật, phân quyền theo phạm vi tổ chức và migration Flyway.

## Cách chạy

Test dùng database riêng `dotani_test` với user `dotani_test` / mật khẩu `dotani_test` (cấu hình trong `src/test/resources/application-test.yml`). Mỗi test tự xoá sạch dữ liệu trước khi chạy, không phụ thuộc dữ liệu seed.

Tạo database và user một lần (MySQL chạy tay ở `D:\Tools\mysql-8.4`, đăng nhập bằng root):

```sql
CREATE DATABASE IF NOT EXISTS dotani_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'dotani_test'@'localhost' IDENTIFIED BY 'dotani_test';
CREATE USER IF NOT EXISTS 'dotani_test'@'127.0.0.1' IDENTIFIED BY 'dotani_test';
GRANT ALL PRIVILEGES ON dotani_test.* TO 'dotani_test'@'localhost';
GRANT ALL PRIVILEGES ON dotani_test.* TO 'dotani_test'@'127.0.0.1';

-- Phase2MigrationTest dựng lại schema trong database riêng này
CREATE DATABASE IF NOT EXISTS dotani_migration_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON dotani_migration_test.* TO 'dotani_test'@'localhost';
GRANT ALL PRIVILEGES ON dotani_migration_test.* TO 'dotani_test'@'127.0.0.1';
```

Chạy:

```powershell
cd D:\Projects\DOTANI\dotani-be
mvn clean verify
```

Dùng MySQL khác thì đặt `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`. Trên GitHub Actions (`.github/workflows/ci.yml`), MySQL được dựng sẵn dưới dạng service với đúng user này.

Luôn chạy kèm `clean` sau khi xoá/đổi tên class: Maven không tự xoá file `.class` cũ trong `target`, và Spring vẫn quét thấy chúng.

## Kết quả gần nhất

`mvn clean verify` trên database trống: **143 test, 0 lỗi**.

| Module | Lớp test | Số test | Nội dung chính |
| --- | --- | ---: | --- |
| auth | `AuthServiceIntegrationTest` | 13 | Đăng ký, đăng nhập, Google login, refresh token, JWT hết hạn/sai |
| auth | `SelfRegisteredMemberScopeIntegrationTest` | 1 | Tài khoản tự đăng ký có phạm vi phường + TDP, xem được sự kiện cấp phường |
| member | `MemberManagementIntegrationTest` | 12 | CRUD đoàn viên theo phạm vi phường / TDP |
| member | `OrganizationIntegrationTest` | 13 | Tổ chức, đăng ký hồ sơ đoàn viên vào TDP |
| member | `MemberBankingIntegrationTest` | 10 | Thông tin ngân hàng, QR (không lộ qua `/uploads`) |
| member | `MemberProfileIntegrationTest` | 6 | Hồ sơ cá nhân, avatar |
| member | `RoleManagementIntegrationTest` | 5 | Chỉ bí thư phường đổi role, role tài khoản được đồng bộ |
| member | `MemberSecurityRegressionTest` | 3 | Chống leo thang quyền |
| member | `MemberDashboardIntegrationTest` | 2 | Thống kê đoàn viên |
| member | `MemberLookupIntegrationTest` | 2 | `POST /api/members/lookup` chỉ trả trường công khai |
| event | `EventIntegrationTest` | 15 | Sự kiện, đăng ký tham gia, phạm vi xem |
| event | `EventAttachmentAndNotificationIntegrationTest` | 4 | File đính kèm sự kiện; thông báo sự kiện cấp phường/TDP |
| event | `EventDashboardIntegrationTest` | 3 | Thống kê sự kiện |
| content | `PostIntegrationTest` | 7 | Bài viết, file đính kèm, giới hạn loại/kích thước file |
| chat | `ChatIntegrationTest` | 9 | Hội thoại, WebSocket STOMP với JWT, chặn người ngoài |
| chat | `ChatReadStateIntegrationTest` | 2 | Đếm tin chưa đọc, đánh dấu đã đọc |
| notification | `NotificationIntegrationTest` | 6 | Danh sách, chưa đọc, đánh dấu đã đọc |
| audit | `AuditLogIntegrationTest` | 4 | Ghi và xem audit log theo role |
| auth | `RoleSingleSourceIntegrationTest` | 2 | Role/phạm vi chỉ lưu ở member; đổi role có hiệu lực ở token kế tiếp |
| member | `MemberEmailIntegrationTest` | 4 | Email lưu một nơi: tài khoản nếu có, ngược lại email liên hệ |
| member | `BankingEncryptionIntegrationTest` | 3 | Số tài khoản mã hoá AES-GCM trong DB |
| audit | `AuditDetailsIntegrationTest` | 2 | Audit ghi giá trị trước/sau, IP, user agent |
| shared | `TimeStorageIntegrationTest` | 3 | Lưu UTC, trả ISO-8601 có Z, lọc theo ngày Việt Nam |
| shared | `SoftDeleteIntegrationTest` | 2 | Xoá mềm: ẩn khỏi truy vấn nhưng giữ lịch sử |
| migration | `Phase2MigrationTest` | 8 | Chạy V801–V809 trên dữ liệu dạng cũ, kiểm tra từng bước chuyển dữ liệu |
| — | `SmokeIntegrationTest` | 2 | App khởi động, health, xác thực cơ bản |

## Viết test mới

Kế thừa `com.dotani.support.IntegrationTest`, rồi dùng:

- `data.standardFixture()`: một phường, hai TDP và một đoàn viên cho mỗi role.
- `auth.as(member, request)`: gắn JWT của đoàn viên đó vào request.
- `json(...)`, `body(result)`: tạo/đọc JSON.
