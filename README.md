# HCMCYU

Backend va huong dan chay development cho he thong website quan ly doan vien Doan TNCS Ho Chi Minh Phuong Thuong Cat.

## Cau truc thu muc

```text
D:\Java\HCMCYU            # Backend Spring Boot multi-module + docker-compose.yml
D:\Java\HCMCYU-frontend   # Frontend React/Vite, duoc compose build tu ../HCMCYU-frontend
```

## Cong nghe

- Backend: Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, Spring Cloud Gateway, WebSocket/STOMP, Maven
- Frontend: React, Vite, TypeScript, React Router, Axios, TanStack Query
- Database: MySQL 8.4
- Runtime dev: Docker Compose

## Service va port

| Service | Port Docker dev | Health |
| --- | ---: | --- |
| frontend | 5173 | `http://localhost:5173/health` |
| api-gateway | 8080 | `http://localhost:8080/api/health` |
| mysql | 3306 | Docker healthcheck |

Trong Docker Compose, cac microservice noi bo khong publish port ra host. Frontend va client chi goi API qua gateway de tranh bypass JWT/identity headers.

API frontend nen goi qua gateway:

```text
http://localhost:8080
```

WebSocket chat qua gateway:

```text
ws://localhost:8080/ws/chat
```

## Chay toan bo bang Docker Compose

Yeu cau:

- Docker Desktop dang chay
- Frontend nam dung tai `D:\Java\HCMCYU-frontend`
- Backend nam tai `D:\Java\HCMCYU`

Tai folder backend:

```powershell
cd D:\Java\HCMCYU
```

Tao file `.env` tu template:

```powershell
Copy-Item .env.example .env
```

Mo `.env` va thay cac gia tri `change-me`:

```powershell
notepad .env
```

Vi du format `.env` cho may local:

```env
MYSQL_ROOT_PASSWORD=<local-root-password>
DB_USERNAME=root
DB_PASSWORD=<local-db-password>
AUTH_JWT_SECRET=<at-least-32-characters-random-secret>
AUTH_INTERNAL_SECRET=<random-internal-secret>
AUDIT_INTERNAL_SECRET=<random-internal-secret>
NOTIFICATION_INTERNAL_SECRET=<random-internal-secret>
DEV_WARD_SECRETARY_PASSWORD=Demo@12345
```

Neu may dang co MySQL/XAMPP dung port `3306`, doi trong `.env`:

```env
MYSQL_PORT=3307
```

Chay toan bo he thong:

```powershell
docker compose up --build
```

Hoac chay nen:

```powershell
docker compose up --build -d
```

Kiem tra trang thai:

```powershell
docker compose ps
```

Tat ca container nen o trang thai `healthy`, frontend co the mat vai giay dau de healthcheck sang `healthy`.

## URL su dung

- Frontend: `http://localhost:5173`
- API Gateway: `http://localhost:8080`
- Gateway health: `http://localhost:8080/api/health`

## Lenh kiem tra nhanh

```powershell
curl.exe -fsS http://localhost:8080/api/health
curl.exe -fsS http://localhost:5173/health
curl.exe -fsS http://localhost:8080/api/auth/health
curl.exe -fsS http://localhost:8080/api/members/health
curl.exe -fsS http://localhost:8080/api/events/health
curl.exe -fsS http://localhost:8080/api/posts/health
curl.exe -fsS http://localhost:8080/api/chat/health
curl.exe -fsS http://localhost:8080/api/notifications/health
```

## Tai khoan demo development

Seed development chi chay voi profile `dev`.

Tai khoan quan tri phuong:

```text
username: admin
email: admin@hcmcyu.local
password: Demo@12345
role: WARD_SECRETARY
ghi chu: tai khoan quyen cao nhat dev, xem/sua/xoa toan bo doan vien va phan quyen role
```

Tai khoan bi thu phuong mau:

```text
username: ward.secretary
email: ward.secretary@hcmcyu.local
password: Demo@12345
role: WARD_SECRETARY
```

Dang nhap qua API:

```powershell
curl.exe -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"usernameOrEmail\":\"admin\",\"password\":\"Demo@12345\"}"
```

Neu ban doi `DEV_WARD_SECRETARY_PASSWORD` trong `.env` va reset volume database, hay dung password moi khi login.

Neu trinh duyet bao `Network Error` khi login:

- Mo frontend bang `http://localhost:5173` hoac `http://127.0.0.1:5173`.
- Gateway dev da allow CORS cho `localhost:*` va `127.0.0.1:*`.
- Kiem tra gateway bang `curl.exe -fsS http://localhost:8080/api/health`.

## Lenh van hanh Docker

Xem log tat ca service:

```powershell
docker compose logs -f
```

Xem log mot service:

```powershell
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs -f member-service
```

Dung stack:

```powershell
docker compose down
```

Dung va xoa volume MySQL/storage de seed lai tu dau:

```powershell
docker compose down -v
```

Build lai rieng frontend:

```powershell
docker compose up --build -d frontend
```

Build lai mot backend service:

```powershell
docker compose up --build -d auth-service
```

## Luu y bao mat

- Khong commit file `.env`.
- `.env.example` chi la template, khong dung password/secret that.
- QR Banking trong he thong chi la du lieu ho so ca nhan, khong co payment service va khong xu ly giao dich thanh toan.
- Frontend chi an/hien UI theo role; backend van la noi enforce JWT, RBAC va organization scope.

## Troubleshooting

Neu port `3306` bi chiem:

```env
MYSQL_PORT=3307
```

Sau do chay lai:

```powershell
docker compose up --build -d
```

Neu Maven download trong Docker bi loi mang tam thoi, chay lai lenh build sau khi ket noi on dinh:

```powershell
docker compose up --build -d
```

Neu container khong healthy:

```powershell
docker compose ps
docker compose logs --tail=200 <service-name>
```

Vi du:

```powershell
docker compose logs --tail=200 member-service
```
