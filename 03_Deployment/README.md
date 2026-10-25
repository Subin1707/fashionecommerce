# Fashion Ecommerce — chạy demo local

Tài liệu này hướng dẫn chạy bản demo trên máy phát triển. Repo hiện **chưa có cấu hình triển khai production** như image/container cho backend và frontend, reverse proxy/TLS, domain, quản lý secrets hoặc quy trình backup/restore production.

## Yêu cầu

- Java 21
- Node.js 20+ và npm
- Docker Desktop với Docker Compose
- Git

Các cổng mặc định: backend `8081`, Vite `5173`, PostgreSQL `5432`, Redis `6379`, Elasticsearch `9200`, RabbitMQ `5672` và giao diện RabbitMQ `15672`.

## 1. Khởi động dịch vụ phụ trợ

Mở terminal tại thư mục gốc của repo:

```powershell
docker compose -f 03_Deployment/docker-compose.yml up -d --wait
docker compose -f 03_Deployment/docker-compose.yml ps
```

Compose khởi động PostgreSQL, Redis, Elasticsearch và RabbitMQ. PostgreSQL demo dùng database/user/password `fashion`/`fashion`/`fashion`. Schema `database/schema.sql` được nạp khi volume PostgreSQL được tạo lần đầu; ứng dụng chạy các migration SQL khi khởi động.

Các thông tin đăng nhập và cấu hình Compose này chỉ dành cho local demo. Đừng đưa chúng lên máy chủ public. Redis được khởi động để tiện thử nghiệm, nhưng ứng dụng hiện cấu hình cache cục bộ (`spring.cache.type=simple`).

## 2. Cấu hình backend

Tại thư mục gốc, tạo `.env` từ mẫu nếu chưa có:

```powershell
if (-not (Test-Path .env)) { Copy-Item 03_Deployment/.env.example .env }
notepad .env
```

Đặt các biến cần dùng trong `.env`:

- `SPRING_DATASOURCE_URL` — mặc định local: `jdbc:postgresql://127.0.0.1:5432/fashion`
- `SPRING_DATASOURCE_USERNAME` và `SPRING_DATASOURCE_PASSWORD` — mặc định Compose là `fashion` và `fashion`
- `FASHION_JWT_SECRET` — thay bằng chuỗi ngẫu nhiên dài ít nhất 32 bytes
- `FASHION_ADMIN_PASSWORD` và `FASHION_CUSTOMER_PASSWORD` — mật khẩu tài khoản demo
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` — cần thiết nếu muốn đăng ký bằng email OTP

Ứng dụng Spring Boot đọc `.env` từ thư mục hiện hành. `scripts/run-backend-current.ps1` cũng nạp các biến `MAIL_*` từ `.env`. Compose **không** tự nạp tệp này. Các tên `DB_HOST`, `DB_PASSWORD`, `REDIS_HOST` trong cấu hình cũ không được backend hiện tại dùng để cấu hình tương ứng; hãy dùng các tên `SPRING_DATASOURCE_*` nêu trên.

Các dịch vụ backend khác đang dùng mặc định local trong `src/main/resources/application.properties`: Elasticsearch `localhost:9200`, Redis `localhost:6379`, RabbitMQ `localhost:5672` với `guest/guest`. Nếu thay đổi chúng, đặt các biến Spring tương ứng (`SPRING_ELASTICSEARCH_URIS`, `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`).

## 3. Chạy backend

Từ thư mục gốc, trong một terminal:

```powershell
.\scripts\run-backend-current.ps1
```

Hoặc:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend mặc định lắng nghe tại <http://localhost:8081>. Script `run-backend-current.ps1` kiểm tra checkout có API campaign event mới và ưu tiên Maven cài sẵn, sau đó dùng Maven Wrapper.

## 4. Chạy frontend

Mở terminal thứ hai:

```powershell
Set-Location frontend
npm install
npm run dev
```

Mở <http://localhost:5173>. Vite chuyển tiếp `/api` và `/uploads` đến backend `localhost:8081`. Bản đồ dùng OpenStreetMap nếu chưa đặt `VITE_CARTO_API_KEY`; có thể tạo `frontend/.env.local` từ `config/frontend-env.example` để cấu hình key tùy chọn.

## 5. Tài khoản demo

Mặc định môi trường local tạo:

| Vai trò | Email | Mật khẩu mặc định |
| --- | --- | --- |
| Admin | `admin@fashion.local` | `Admin@123` |
| Khách hàng | `customer@fashion.local` | `Customer@123` |

Đặt `FASHION_ADMIN_PASSWORD` và `FASHION_CUSTOMER_PASSWORD` trước khi khởi động nếu muốn đổi mật khẩu. Đây là tài khoản demo; không sử dụng thông tin mặc định ở môi trường public/production.

## 6. Kiểm thử

Từ thư mục gốc:

```powershell
.\mvnw.cmd test
```

Lệnh trên chạy bộ test backend hiện có. Tài liệu này không ghi số test cố định vì kết quả thay đổi theo phiên bản mã nguồn.

## 7. Dừng dịch vụ

Từ thư mục gốc:

```powershell
docker compose -f 03_Deployment/docker-compose.yml down
```

Lệnh giữ nguyên volume dữ liệu. Chỉ dùng `down -v` khi chủ động muốn xóa dữ liệu PostgreSQL, Elasticsearch và RabbitMQ của môi trường demo.

## Tệp cấu hình liên quan

- `03_Deployment/docker-compose.yml` — PostgreSQL, Redis, Elasticsearch, RabbitMQ cho local demo.
- `03_Deployment/.env.example` — mẫu biến môi trường backend.
- `03_Deployment/config/application-example.properties` — ví dụ cấu hình Spring; ứng dụng không tự nạp tệp này.
- `03_Deployment/config/frontend-env.example` — biến tùy chọn cho frontend.
- `src/main/resources/application.properties` — cấu hình Spring Boot đang được ứng dụng sử dụng.