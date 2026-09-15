# DOTANI Backend

Backend cho hệ thống website quản lý đoàn viên Đoàn TNCS Hồ Chí Minh Phường Thượng Cát.

Frontend repository: https://github.com/nvtquang/dotani-fe

## Mô tả

DOTANI quản lý hồ sơ đoàn viên theo tổ dân phố, đăng nhập/phân quyền, sự kiện, bài viết/báo cáo hoạt động, chat realtime, thông báo và dashboard.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Security, JWT, BCrypt
- Spring Data JPA, Flyway
- Spring Cloud Gateway
- WebSocket/STOMP
- MySQL 8.4
- Maven multi-module
- Docker Compose
- OpenAPI/Swagger

## Scope Backend

Các service chính:

| Service | Vai trò |
| --- | --- |
| `api-gateway` | Gateway, routing, CORS, WebSocket proxy |
| `auth-service` | Register, login, refresh token, Google login, JWT |
| `member-service` | Tổ dân phố, đoàn viên, hồ sơ cá nhân, avatar, QR Banking, phân quyền role |
| `event-service` | Sự kiện, đại hội, họp, đăng ký tham gia |
| `content-service` | Bài viết, thông báo, báo cáo hoạt động, ảnh |
| `chat-service` | Chat 1-1, group chat, WebSocket realtime |
| `notification-service` | Thông báo, unread count, mark read |
| `audit-service` | Audit log các thao tác quản trị quan trọng |

Role đang dùng:

- `WARD_SECRETARY`
- `WARD_DEPUTY_SECRETARY`
- `TDP_SECRETARY`
- `TDP_DEPUTY_SECRETARY`
- `MEMBER`

Nguyên tắc quan trọng:

- `WARD_SECRETARY` có quyền toàn phường và là role duy nhất được phân quyền role.
- `WARD_DEPUTY_SECRETARY` có quyền nghiệp vụ toàn phường nhưng không được đổi role.
- `TDP_SECRETARY` và `TDP_DEPUTY_SECRETARY` chỉ thao tác trong TDP của mình.
- `MEMBER` chỉ thao tác dữ liệu cá nhân được phép.
- Backend enforce JWT, RBAC và organization scope. Frontend chỉ ẩn/hiện UI.

## Cài đặt

Yêu cầu:

- Docker Desktop
- Git

Clone backend và frontend:

```powershell
cd D:\Java
git clone https://github.com/nvtquang/dotani-be.git 
git clone https://github.com/nvtquang/dotani-fe.git 
```

Tạo file môi trường:

```powershell
cd D:\Java\HCMCYU
Copy-Item .env.example .env
notepad .env
```

Các biến cần thay tối thiểu:

```env
MYSQL_ROOT_PASSWORD=your-local-root-password
DB_USERNAME=root
DB_PASSWORD=your-local-db-password

AUTH_JWT_SECRET=change-to-at-least-32-random-characters
AUTH_INTERNAL_SECRET=change-me
AUDIT_INTERNAL_SECRET=change-me
NOTIFICATION_INTERNAL_SECRET=change-me

DEV_WARD_SECRETARY_PASSWORD=Demo@12345

FRONTEND_URL=http://localhost:5173
VITE_API_BASE_URL=http://localhost:8080
VITE_CHAT_WS_URL=ws://localhost:8080/ws/chat
```

Nếu dùng đăng nhập Google, thêm OAuth Client ID:

```env
GOOGLE_CLIENT_ID=your-google-oauth-client-id.apps.googleusercontent.com
VITE_GOOGLE_CLIENT_ID=your-google-oauth-client-id.apps.googleusercontent.com
```

Trong Google Cloud Console, OAuth Client cần có Authorized JavaScript origin:

```text
http://localhost:5173
```

Chạy toàn bộ hệ thống:

```powershell
docker compose up --build
```

Chạy nền:

```powershell
docker compose up --build -d
```

Kiểm tra container:

```powershell
docker compose ps
```

## URL chính

- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- Gateway health: http://localhost:8080/api/health
- Gateway WebSocket chat: `ws://localhost:8080/ws/chat`

## Tài khoản mẫu

Seed development chỉ chạy với profile `dev`.

Mật khẩu mặc định lấy từ `DEV_WARD_SECRETARY_PASSWORD`, mặc định:

```text
Demo@12345
```

Một số tài khoản mẫu:

| Username | Email | Role | Ghi chú |
| --- | --- | --- | --- |
| `admin` | `admin@hcmcyu.local` | `WARD_SECRETARY` | Tài khoản quyền cao nhất dev |
| `ward.secretary` | `ward.secretary@hcmcyu.local` | `WARD_SECRETARY` | Bí thư phường |
| `ward.deputy` | `ward.deputy@hcmcyu.local` | `WARD_DEPUTY_SECRETARY` | Phó bí thư phường |
| `tdp1.secretary` | `tdp1.secretary@hcmcyu.local` | `TDP_SECRETARY` | Bí thư TDP 1 |
| `tdp1.deputy` | `tdp1.deputy@hcmcyu.local` | `TDP_DEPUTY_SECRETARY` | Phó bí thư TDP 1 |
| `tdp1.member1` | `tdp1.member1@hcmcyu.local` | `MEMBER` | Đoàn viên TDP 1 |

Các TDP khác có cùng quy ước:

```text
tdp2.secretary
tdp2.deputy
tdp2.member1
...
tdp5.secretary
tdp5.deputy
tdp5.member5
```

Test login nhanh:

```powershell
curl.exe -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"usernameOrEmail\":\"admin\",\"password\":\"Demo@12345\"}"
```

## Lệnh hữu ích

Xem log:

```powershell
docker compose logs -f
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs -f member-service
```

Dừng hệ thống:

```powershell
docker compose down
```

Dừng và xóa volume để seed lại từ đầu:

```powershell
docker compose down -v
```

Build lại một service:

```powershell
docker compose up --build -d auth-service
docker compose up --build -d frontend
```

Chạy test backend:

```powershell
mvn test
```

## Lưu ý

- Không commit file `.env`.
- Không hard-code password, JWT secret, Google Client ID vào code.
- Nếu máy đang có MySQL/XAMPP dùng port `3306`, đổi `MYSQL_PORT=3307` trong `.env`.
- Nếu đổi `VITE_GOOGLE_CLIENT_ID`, cần build lại frontend vì biến `VITE_*` được bake vào bundle.
- Khi trình duyệt báo `Network Error`, kiểm tra gateway trước: `http://localhost:8080/api/health`.
- Khi cần reset dữ liệu seed, dùng `docker compose down -v` rồi chạy lại `docker compose up --build`.