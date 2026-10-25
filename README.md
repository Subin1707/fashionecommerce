# README — HỆ THỐNG FASHION

## 1. Thông tin nhóm

- **Tên nhóm:** Nhóm 5
- **Tên đề tài:** Hệ thống website thương mại điện tử thời trang ( Gợi ý kích cỡ )

| Thành viên | Mã sinh viên | Vai trò |
| --- | --- | --- |
| Nguyễn Mạnh Quyền | 23010198 | Leader |
| Nguyễn Minh Nguyệt | 23010408 | Thành viên |
| Nguyễn Hữu Huy | 23010240 | Thành viên |
| Nguyễn Trọng Dương | 23010227 | Thành viên |

## 2. Địa chỉ truy cập

| Thành phần | Địa chỉ |
| --- | --- |
| Frontend local | <http://localhost:5173> |
| Backend API local | <http://localhost:8081> |
| PostgreSQL | `localhost:5432` |
| Elasticsearch | <http://localhost:9200> |
| RabbitMQ Management | <http://localhost:15672> |

**Địa chỉ public/tunnel:** Chưa có; hiện hướng dẫn này dành cho chạy thử trên máy local.

## 3. Tài khoản demo

| Vai trò | Email | Mật khẩu mặc định |
| --- | --- | --- |
| Admin | `admin@fashion.local` | `Admin@123` |
| Khách hàng | `customer@fashion.local` | `Customer@123` |

Đây là thông tin phục vụ nghiệm thu local. Không dùng mật khẩu mặc định ở môi trường public hoặc production. Có thể thay bằng `FASHION_ADMIN_PASSWORD` và `FASHION_CUSTOMER_PASSWORD` trong `.env`.

## 4. Yêu cầu môi trường

- Java 21
- Node.js 20 trở lên và npm
- Docker Desktop có Docker Compose
- Git

## 5. Hướng dẫn chạy nhanh trên Windows

Mở PowerShell tại thư mục gốc repository.

### Bước 1: Khởi động dịch vụ phụ trợ

```powershell
docker compose -f 03_Deployment/docker-compose.yml up -d --wait
docker compose -f 03_Deployment/docker-compose.yml ps
```

Compose khởi động PostgreSQL, Redis, Elasticsearch và RabbitMQ. PostgreSQL local dùng database/user/password `fashion`/`fashion`/`fashion`; cấu hình này chỉ dành cho demo. Schema được nạp khi volume PostgreSQL được tạo lần đầu.

### Bước 2: Tạo cấu hình backend

Tạo `.env` ở thư mục gốc dựa trên các thiết lập mặc định trong `03_Deployment/config/application-example.properties`. Nếu cần gửi email OTP, cấu hình SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`). Đặt `FASHION_JWT_SECRET` thành chuỗi ngẫu nhiên tối thiểu 32 bytes. Các giá trị mặc định của demo không phù hợp để đưa lên máy chủ public.

Ứng dụng đọc `.env` từ thư mục hiện hành. Compose không tự đọc tệp `.env` này.

### Bước 3: Chạy backend

Từ thư mục gốc, trong một terminal:

```powershell
.\scripts\run-backend-current.ps1
```

Hoặc dùng Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend mặc định chạy tại <http://localhost:8081>.

### Bước 4: Chạy frontend

Mở terminal khác:

```powershell
Set-Location frontend
npm install
npm run dev
```

Mở <http://localhost:5173>. Vite chuyển tiếp `/api` và `/uploads` tới backend tại `localhost:8081`.

### Bước 5: Chạy kiểm thử backend

Từ thư mục gốc:

```powershell
.\mvnw.cmd test
```

Số lượng và kết quả kiểm thử phụ thuộc phiên bản mã nguồn hiện tại; xem kết quả Maven vừa chạy để biết trạng thái chính xác.

### Bước 6: Dừng dịch vụ phụ trợ

```powershell
docker compose -f 03_Deployment/docker-compose.yml down
```

Lệnh này giữ lại dữ liệu trong volume. Chỉ dùng `down -v` khi muốn xóa dữ liệu demo.

## 6. Cấu hình

Các biến backend có thể đặt trong `.env` ở thư mục gốc:

| Biến | Mục đích |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL PostgreSQL; mặc định `jdbc:postgresql://127.0.0.1:5432/fashion` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Tài khoản PostgreSQL; mặc định Compose là `fashion` / `fashion` |
| `FASHION_JWT_SECRET` | Khóa ký JWT, tối thiểu 32 bytes |
| `FASHION_JWT_EXPIRATION_MS` | Thời hạn JWT, mặc định 24 giờ |
| `FASHION_ADMIN_PASSWORD` / `FASHION_CUSTOMER_PASSWORD` | Mật khẩu các tài khoản demo |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP gửi mã OTP đăng ký |
| `SPRING_ELASTICSEARCH_URIS` | Địa chỉ Elasticsearch |
| `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` | Địa chỉ Redis |
| `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` | Kết nối RabbitMQ |

Frontend có biến tùy chọn `VITE_CARTO_API_KEY`; có thể tham khảo `03_Deployment/config/frontend-env.example` và đặt trong `frontend/.env.local`. Bản đồ vẫn dùng OpenStreetMap nếu không có key.

## 7. Tài liệu và cấu hình liên quan

- [Hướng dẫn chạy demo và triển khai local](03_Deployment/README.md)
- [Compose dịch vụ phụ trợ](03_Deployment/docker-compose.yml)
- [Ví dụ cấu hình Spring](03_Deployment/config/application-example.properties)
- [Ví dụ biến môi trường frontend](03_Deployment/config/frontend-env.example)
- [Cấu hình Spring Boot đang sử dụng](src/main/resources/application.properties)
- [Thiết lập email OTP](OTP_SETUP.md)
- [Kiểm thử hiệu năng](PERFORMANCE_TESTING.md)
- [Kiểm thử bảo mật](SECURITY_TESTING.md)
- [Tài liệu cơ sở dữ liệu](database/README.md)
- [README frontend](frontend/README.md)

> Repository hiện chưa cung cấp cấu hình triển khai production như image backend/frontend, domain, TLS/reverse proxy, quản lý secrets và quy trình backup/restore. `03_Deployment/.env.example`, `03_Deployment/HUONG_DAN_CAI_DAT.md` và `03_Deployment/YEU_CAU_MOI_TRUONG.md` hiện không có trong checkout này.
