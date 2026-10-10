# Deploy backend DOTANI lên Render

Backend là **một ứng dụng Spring Boot duy nhất** (`dotani-monolith`) cùng một database MySQL. Frontend deploy riêng trên Vercel và gọi thẳng vào backend.

## Kiến trúc

| Thành phần | Nơi chạy | Ghi chú |
| --- | --- | --- |
| Backend `dotani-monolith` | Render **Web Service** (Docker, `Dockerfile` ở gốc repo) | Public, phục vụ REST `/api/**`, WebSocket `/ws/chat`, file `/uploads/**` |
| MySQL 8.4 | MySQL managed bên ngoài (Aiven, Railway, DigitalOcean...) | Một database, ví dụ `dotani_db`. Render không có MySQL managed |
| File upload | Render **Persistent Disk** mount vào `/app/storage` | Không có disk thì file mất sau mỗi lần redeploy |
| Frontend | Vercel | Xem bước 4 |

## Bước 1: Chuẩn bị MySQL

Tạo một database rỗng (Flyway tự tạo bảng khi app khởi động lần đầu):

```sql
CREATE DATABASE dotani_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

JDBC URL có dạng:

```text
jdbc:mysql://<host>:<port>/dotani_db?useSSL=true&serverTimezone=Asia/Ho_Chi_Minh
```

Nếu provider yêu cầu chứng chỉ/tuỳ chọn SSL riêng, chỉnh URL theo hướng dẫn của provider.

## Bước 2: Tạo Web Service

Render Dashboard → **New → Web Service** → chọn repo backend.

- Runtime: `Docker`, Dockerfile Path: `Dockerfile`
- Health Check Path: `/actuator/health`
- Disk: thêm Persistent Disk, Mount Path `/app/storage`

Environment variables:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<host>:<port>/dotani_db?useSSL=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
AUTH_JWT_SECRET=<chuỗi ngẫu nhiên, tối thiểu 32 ký tự>
APP_CORS_ALLOWED_ORIGINS=https://<domain-vercel>
GOOGLE_CLIENT_ID=<google-oauth-client-id>      # chỉ khi dùng đăng nhập Google
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0
AVATAR_STORAGE_PATH=/app/storage/avatars
BANK_QR_STORAGE_PATH=/app/storage/bank-qr
POST_IMAGE_STORAGE_PATH=/app/storage/post-images
EVENT_ATTACHMENT_STORAGE_PATH=/app/storage/event-attachments
STORAGE_CHAT_ATTACHMENT_PATH=/app/storage/chat-attachments
STORAGE_CHAT_GROUP_AVATAR_PATH=/app/storage/chat-group-avatars
```

Render tự cấp biến `PORT`; app đọc `${PORT:8080}` nên không cần khai báo.

Profile `prod`:

- **Không có giá trị mặc định** cho `DB_*`, `AUTH_JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`. Thiếu biến nào, app dừng ngay khi khởi động với lỗi `Could not resolve placeholder '<TÊN_BIẾN>'`.
- Swagger/OpenAPI tắt. Muốn bật tạm thời: `SPRINGDOC_ENABLED=true`.
- Không chạy seed dữ liệu mẫu (seed chỉ có ở profile `dev`).

## Bước 3: Khởi tạo dữ liệu ban đầu (chỉ làm một lần)

Vì `prod` không seed, database mới **chưa có phường/TDP và chưa có tài khoản quản trị**. Người dùng không đăng ký được cho tới khi có TDP.

1. Tạo phường và các TDP:

   ```sql
   INSERT INTO organization_units (id, name, code, type, parent_id, active, created_at, updated_at)
   VALUES ('ward-thuong-cat', 'Đoàn phường Thượng Cát', 'THUONG_CAT', 'WARD', NULL, TRUE, NOW(), NOW());

   INSERT INTO organization_units (id, name, code, type, parent_id, active, created_at, updated_at)
   VALUES ('tdp-1', 'Chi đoàn TDP 1', 'TDP_1', 'YOUTH_UNION_BRANCH', 'ward-thuong-cat', TRUE, NOW(), NOW());
   -- lặp lại cho các TDP khác
   ```

2. Người sẽ làm Bí thư phường tự đăng ký tài khoản trên frontend (chọn một TDP bất kỳ).

3. Nâng tài khoản đó lên `WARD_SECRETARY` và gắn vào phường:

   ```sql
   UPDATE user_accounts SET role = 'WARD_SECRETARY', organization_id = 'ward-thuong-cat', tdp_id = NULL
   WHERE username = '<username>';

   UPDATE members m JOIN user_accounts u ON u.member_id = m.id
   SET m.member_role = 'WARD_SECRETARY', m.organization_id = 'ward-thuong-cat'
   WHERE u.username = '<username>';
   ```

   Người đó đăng nhập lại để nhận quyền mới. Từ đây các role khác được cấp qua giao diện.

## Bước 4: Cấu hình frontend trên Vercel

```env
VITE_API_BASE_URL=https://<backend>.onrender.com
VITE_CHAT_WS_URL=wss://<backend>.onrender.com/ws/chat
VITE_GOOGLE_CLIENT_ID=<google-oauth-client-id>
```

Redeploy frontend sau khi đổi biến (`VITE_*` được đóng vào bundle lúc build). Nếu dùng Google login, thêm `https://<domain-vercel>` vào Authorized JavaScript origins trong Google Cloud Console.

## Bước 5: Kiểm tra sau deploy

```powershell
curl.exe https://<backend>.onrender.com/api/health
curl.exe https://<backend>.onrender.com/actuator/health
```

Sau đó trên frontend: đăng nhập, mở bảng tin, danh sách đoàn viên, chat, và thử upload avatar.

## Lỗi thường gặp

- **App không khởi động, log có `Could not resolve placeholder`**: thiếu biến môi trường được nêu trong lỗi.
- **`Network Error` trên frontend**: `VITE_API_BASE_URL` chưa trỏ đúng URL Render, hoặc domain frontend chưa có trong `APP_CORS_ALLOWED_ORIGINS`.
- **WebSocket không kết nối**: `VITE_CHAT_WS_URL` phải dùng `wss://.../ws/chat`.
- **File upload mất sau redeploy**: chưa gắn Persistent Disk vào `/app/storage`.
- **Google login lỗi**: `GOOGLE_CLIENT_ID` (backend) và `VITE_GOOGLE_CLIENT_ID` (frontend) phải cùng một OAuth Client ID.
