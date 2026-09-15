# Deploy Backend DOTANI/HCMCYU lên Render

Tài liệu này hướng dẫn deploy backend Spring Boot microservice lên Render. Frontend vẫn nên deploy Vercel, còn frontend sẽ gọi API qua `api-gateway` public của Render.

## Kiến trúc đề xuất

Render nên chạy mỗi microservice là một service riêng. Root `Dockerfile` mặc định build `api-gateway`; nếu dùng cùng root Dockerfile cho service khác, set Docker build arg `MODULE=<module-name>`. Bạn cũng có thể dùng Dockerfile riêng có sẵn trong từng thư mục service.

| Service | Render type | Public? |
| --- | --- | --- |
| `api-gateway` | Web Service | Có |
| `auth-service` | Private Service | Không |
| `member-service` | Private Service | Không |
| `event-service` | Private Service | Không |
| `content-service` | Private Service | Không |
| `chat-service` | Private Service | Không |
| `notification-service` | Private Service | Không |
| `audit-service` | Private Service | Không |
| MySQL | External managed MySQL hoặc MySQL Docker + Persistent Disk | Không nên public |

Lý do: frontend chỉ gọi `api-gateway`; các service còn lại giao tiếp qua Render Private Network.

## Lưu ý quan trọng

- Render cung cấp biến `PORT`; backend đã đọc dạng `${PORT:8080}` hoặc `${PORT:<local-port>}` nên local vẫn chạy như cũ.

```env
PORT=8080
```

- Render hỗ trợ Docker deploy từ Dockerfile.
- Render Private Service không có public URL, chỉ có internal address trong cùng region/workspace.
- Render không có managed MySQL mặc định như Postgres. Nên dùng MySQL managed bên ngoài như Aiven, Railway, DigitalOcean, PlanetScale hoặc tự chạy MySQL Docker trên Render kèm Persistent Disk.
- Nếu dùng upload local storage, các service cần Persistent Disk, nếu không file sẽ mất khi redeploy/restart.

Nguồn Render:

- Web Service: https://render.com/docs/web-services
- Docker: https://render.com/docs/docker
- Private Service: https://render.com/docs/private-services
- Private Network: https://render.com/docs/private-network
- Persistent Disk: https://render.com/docs/disks

## Bước 1: Chuẩn bị database MySQL

Khuyến nghị dùng MySQL managed bên ngoài Render.

Tạo các database:

```text
hcmcyu_auth_db
hcmcyu_member_db
hcmcyu_event_db
hcmcyu_content_db
hcmcyu_chat_db
hcmcyu_notification_db
hcmcyu_audit_db
```

Mỗi service sẽ có `DB_URL` riêng:

```env
jdbc:mysql://<mysql-host>:<mysql-port>/<db-name>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
```

Nếu provider MySQL yêu cầu CA/cert hoặc option SSL riêng, chỉnh URL theo hướng dẫn của provider đó.

## Bước 2: Push code backend lên GitHub

Render nên deploy từ GitHub repo backend.

```powershell
cd D:\Java\HCMCYU
git status
git add .
git commit -m "Prepare backend for Render deploy"
git push
```

## Bước 3: Tạo các secret dùng chung

Tạo các giá trị secret đủ dài:

```env
AUTH_JWT_SECRET=<random-at-least-32-chars>
AUTH_INTERNAL_SECRET=<random-secret>
AUDIT_INTERNAL_SECRET=<random-secret>
NOTIFICATION_INTERNAL_SECRET=<random-secret>
GOOGLE_CLIENT_ID=<google-client-id>
DEV_WARD_SECRETARY_PASSWORD=<demo-password>
```

Không dùng giá trị `change-me` ở production.

## Bước 4: Tạo Private Service cho từng microservice

Vào Render Dashboard:

```text
New -> Private Service -> Connect GitHub repo backend
```

Chọn:

- Runtime/Language: `Docker`
- Branch: `main` hoặc branch bạn deploy
- Region: chọn cùng một region cho tất cả service
- Dockerfile Path: theo từng service

Tạo lần lượt:

### audit-service

```text
Name: audit-service
Dockerfile Path: audit-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8087
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_audit_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
AUDIT_INTERNAL_SECRET=<same-secret>
```

Health Check Path:

```text
/actuator/health
```

### auth-service

```text
Name: auth-service
Dockerfile Path: auth-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8081
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_auth_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
AUTH_JWT_SECRET=<same-jwt-secret>
AUTH_INTERNAL_SECRET=<same-auth-internal-secret>
GOOGLE_CLIENT_ID=<google-client-id>
MEMBER_SERVICE_URL=http://<member-service-internal-address>
```

Health Check Path:

```text
/actuator/health
```

Sau khi tạo `member-service`, quay lại sửa `MEMBER_SERVICE_URL` bằng internal address thật của member-service.

### member-service

```text
Name: member-service
Dockerfile Path: member-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8082
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_member_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
AUTH_SERVICE_URL=http://<auth-service-internal-address>
AUTH_INTERNAL_SECRET=<same-auth-internal-secret>
AUDIT_SERVICE_URL=http://<audit-service-internal-address>
AUDIT_INTERNAL_SECRET=<same-audit-internal-secret>
AVATAR_STORAGE_PATH=/app/storage/avatars
BANK_QR_STORAGE_PATH=/app/storage/bank-qr
```

Disk nếu muốn giữ avatar/QR sau restart:

```text
Mount Path: /app/storage
Size: tùy nhu cầu, ví dụ 1GB
```

Health Check Path:

```text
/actuator/health
```

### event-service

```text
Name: event-service
Dockerfile Path: event-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8083
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_event_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
AUDIT_SERVICE_URL=http://<audit-service-internal-address>
AUDIT_INTERNAL_SECRET=<same-audit-internal-secret>
```

Health Check Path:

```text
/actuator/health
```

### content-service

```text
Name: content-service
Dockerfile Path: content-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8084
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_content_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
AUDIT_SERVICE_URL=http://<audit-service-internal-address>
AUDIT_INTERNAL_SECRET=<same-audit-internal-secret>
POST_IMAGE_STORAGE_PATH=/app/storage/post-images
```

Disk nếu muốn giữ ảnh bài viết:

```text
Mount Path: /app/storage
```

Health Check Path:

```text
/actuator/health
```

### chat-service

```text
Name: chat-service
Dockerfile Path: chat-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8085
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_chat_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
JWT_SECRET=<same-jwt-secret>
MEMBER_SERVICE_URL=http://<member-service-internal-address>
AUTH_INTERNAL_SECRET=<same-auth-internal-secret>
STORAGE_CHAT_ATTACHMENT_PATH=/app/storage/chat-attachments
STORAGE_CHAT_GROUP_AVATAR_PATH=/app/storage/chat-group-avatars
```

Disk nếu muốn giữ file/ảnh chat:

```text
Mount Path: /app/storage
```

Health Check Path:

```text
/actuator/health
```

### notification-service

```text
Name: notification-service
Dockerfile Path: notification-service/Dockerfile
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker,dev
PORT=8086
DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/hcmcyu_notification_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
JWT_SECRET=<same-jwt-secret>
NOTIFICATION_INTERNAL_SECRET=<same-notification-secret>
```

Health Check Path:

```text
/actuator/health
```

## Bước 5: Tạo api-gateway Web Service public

Vào Render Dashboard:

```text
New -> Web Service -> Connect GitHub repo backend
```

Chọn:

```text
Runtime: Docker
Dockerfile Path: ./Dockerfile
Name: api-gateway
```

Env:

```env
SPRING_PROFILES_ACTIVE=docker
PORT=8080
AUTH_JWT_SECRET=<same-jwt-secret>
AUTH_SERVICE_URL=http://<auth-service-internal-address>
MEMBER_SERVICE_URL=http://<member-service-internal-address>
EVENT_SERVICE_URL=http://<event-service-internal-address>
CONTENT_SERVICE_URL=http://<content-service-internal-address>
CHAT_SERVICE_URL=http://<chat-service-internal-address>
CHAT_SERVICE_WS_URL=ws://<chat-service-internal-address>
NOTIFICATION_SERVICE_URL=http://<notification-service-internal-address>
AUDIT_SERVICE_URL=http://<audit-service-internal-address>
FRONTEND_URL=https://<your-vercel-domain>
APP_CORS_ALLOWED_ORIGINS=https://<your-vercel-domain>,http://localhost:5173
```

Health Check Path:

```text
/actuator/health
```

Sau khi deploy xong, bạn sẽ có URL dạng:

```text
https://api-gateway-xxxx.onrender.com
```

Đây là backend public URL để cấu hình cho frontend.

## Bước 6: Cấu hình frontend Vercel

Trong Vercel Project Settings -> Environment Variables:

```env
VITE_API_BASE_URL=https://api-gateway-xxxx.onrender.com
VITE_CHAT_WS_URL=wss://api-gateway-xxxx.onrender.com/ws/chat
VITE_GOOGLE_CLIENT_ID=<google-client-id>
```

Sau đó redeploy frontend trên Vercel.

Nếu dùng Google login, vào Google Cloud Console thêm Authorized JavaScript origin:

```text
https://<your-vercel-domain>
```

## Bước 7: Test sau deploy

Test health gateway:

```powershell
curl.exe https://api-gateway-xxxx.onrender.com/api/health
curl.exe https://api-gateway-xxxx.onrender.com/actuator/health
```

Test login:

```powershell
curl.exe -X POST https://api-gateway-xxxx.onrender.com/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"usernameOrEmail\":\"admin\",\"password\":\"Demo@12345\"}"
```

Test trên frontend:

1. Login bằng tài khoản mẫu.
2. Mở dashboard.
3. Mở danh sách đoàn viên.
4. Mở chat.
5. Upload avatar hoặc ảnh chat nếu đã gắn Persistent Disk.

## Checklist lỗi thường gặp

- `Network Error` trên frontend: kiểm tra `VITE_API_BASE_URL` đã là URL Render public chưa.
- WebSocket không kết nối: kiểm tra `VITE_CHAT_WS_URL` dùng `wss://.../ws/chat`.
- 401 sau login: kiểm tra `AUTH_JWT_SECRET` ở auth-service, gateway, chat-service, notification-service phải giống nhau.
- Service gọi nhau lỗi: kiểm tra internal address trong Render Connect -> Internal.
- File upload mất sau redeploy: service upload cần Persistent Disk tại `/app/storage`.
- Google login lỗi: kiểm tra `GOOGLE_CLIENT_ID` backend và `VITE_GOOGLE_CLIENT_ID` frontend cùng một OAuth Client ID.
