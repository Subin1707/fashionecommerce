# YEU CAU MOI TRUONG

## 1. Phan mem can cai dat

| Thanh phan | Phien ban khuyen nghi | Muc dich |
| --- | --- | --- |
| Java JDK | 21 | Chay backend Spring Boot |
| Maven | 3.9+ hoac Maven Wrapper | Build va test backend |
| Node.js | 20+ | Chay frontend React/Vite |
| npm | Di kem Node.js | Cai dependency frontend |
| Docker Desktop | Ban moi on dinh | Chay PostgreSQL, Redis, Elasticsearch, RabbitMQ |
| Docker Compose | Di kem Docker Desktop | Quan ly cac service Docker |
| Git | Ban moi on dinh | Clone source code |

## 2. Cong mac dinh

| Dich vu | Cong |
| --- | --- |
| Backend Spring Boot | 8081 |
| Frontend Vite | 5173 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Elasticsearch | 9200 |
| RabbitMQ | 5672 |
| RabbitMQ Management UI | 15672 |

Truoc khi chay, can dam bao cac cong tren chua bi ung dung khac chiem dung.

## 3. Bien moi truong quan trong

- `FASHION_ADMIN_PASSWORD`
- `FASHION_CUSTOMER_PASSWORD`
- `FASHION_JWT_SECRET`
- `VNPAY_TMN_CODE`
- `VNPAY_HASH_SECRET`
- `VNPAY_RETURN_URL`
- `VNPAY_FRONTEND_RETURN_URL`
- `GEOCODING_PROVIDER`
- `GEOCODING_URL`
- `ROUTING_URL`
- `MAPS_USER_AGENT`
- `SHIPPING_SIMULATOR_ENABLED`
- `VITE_CARTO_API_KEY`

Khong commit hoac nop secret that. Chi su dung gia tri mau trong tai lieu.

## 4. Ghi chu

- Lan dau chay Maven co the can internet de tai dependency tu Maven Central.
- Lan dau chay `npm install` can internet de tai package frontend.
- Docker Desktop phai dang chay truoc khi thuc hien `docker compose up`.
