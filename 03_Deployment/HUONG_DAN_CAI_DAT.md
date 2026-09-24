# HUONG DAN CAI DAT HE THONG FASHION

## 1. Yeu cau moi truong

- Java 21
- Node.js va npm
- Maven hoac Maven Wrapper cua du an
- Docker Desktop va Docker Compose
- Git

Chi tiet xem `YEU_CAU_MOI_TRUONG.md`.

## 2. Lay ma nguon

```powershell
git clone <repository-url>
cd fashion
```

Neu da co source code, mo terminal tai thu muc goc du an.

## 3. Cau hinh moi truong

Tham khao cac tep mau:

- `03_Deployment/.env.example`
- `03_Deployment/config/application-example.properties`
- `03_Deployment/config/frontend-env.example`

Khong su dung secret that trong tep nop. Khi chay that, tao tep cau hinh rieng tren may trien khai va thay cac gia tri placeholder.

## 4. Khoi dong cac dich vu Docker

Tai thu muc goc du an hoac thu muc co `docker-compose.yml`:

```powershell
docker compose up -d --wait
```

Kiem tra trang thai:

```powershell
docker compose ps
```

He thong Docker khoi dong cac dich vu phu tro:

- PostgreSQL
- Redis
- Elasticsearch
- RabbitMQ

## 5. Chay Backend

Tai thu muc goc du an:

```powershell
.\mvnw.cmd spring-boot:run
```

Neu dung Maven cai san:

```powershell
mvn spring-boot:run
```

Backend mac dinh chay tai:

```text
http://localhost:8081
```

## 6. Chay Frontend

```powershell
cd frontend
npm install
npm run dev
```

Mo dia chi Vite hien thi tren terminal, thong thuong:

```text
http://localhost:5173
```

## 7. Tai khoan mau

| Vai tro | Email | Mat khau |
| --- | --- | --- |
| Admin | admin@fashion.local | Admin@123 |
| Khach hang | customer@fashion.local | Customer@123 |

## 8. Chay kiem thu Backend

Tai thu muc goc du an:

```powershell
.\mvnw.cmd test
```

Ket qua kiem thu gan nhat:

```text
Tests run: 88
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 9. Dung he thong Docker

```powershell
docker compose down
```

Lenh nay dung va xoa container, giu lai volume du lieu Docker.
