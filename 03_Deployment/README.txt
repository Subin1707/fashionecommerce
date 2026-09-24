README - HE THONG FASHION
=========================

1. THONG TIN NHOM
-----------------
Ten nhom: Nhom 5

Thanh vien:
- <Nguyen Manh Quyen> - <23010198> - <Leader>
- <Nguyen Minh Nguyet> - <23010408> - <Thanh vien>
- <Nguyen Huu Huy> - <23010240> - <Thanh vien>
- <Nguyen Trong Duong> - <23010227> - <Thanh vien>
Ten de tai:
He thong website thuong mai dien tu thoi trang Fashion.


2. DIA CHI TRIEN KHAI THU
-------------------------
Backend local:
http://localhost:8081

Frontend local:
http://localhost:5173

Dia chi public/tunnel neu co:
<Dien URL trien khai thu neu co>

Ghi chu:
Neu khong co server public, co the chay thu tren may cuc bo theo huong dan nhanh ben duoi.


3. TAI KHOAN DANG NHAP
----------------------
Admin:
Email: admin@fashion.local
Mat khau: Admin@123

Khach hang:
Email: customer@fashion.local
Mat khau: Customer@123

Ghi chu:
Day la tai khoan demo de nghiem thu. Khong su dung mat khau nay cho moi truong production.


4. HUONG DAN CHAY NHANH
-----------------------
Yeu cau:
- Java 21
- Node.js va npm
- Docker Desktop va Docker Compose
- Git

Buoc 1: Khoi dong cac dich vu phu tro
Tai thu muc goc du an:

docker compose up -d --wait

Kiem tra:

docker compose ps

Buoc 2: Chay backend
Tai thu muc goc du an:

.\mvnw.cmd spring-boot:run

Hoac neu da cai Maven:

mvn spring-boot:run

Buoc 3: Chay frontend

cd frontend
npm install
npm run dev

Mo trinh duyet tai dia chi Vite hien thi, thong thuong:

http://localhost:5173

Buoc 4: Chay kiem thu backend
Tai thu muc goc du an:

.\mvnw.cmd test

Ket qua kiem thu gan nhat:
Tests run: 88
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

Buoc 5: Dung cac dich vu Docker
Tai thu muc goc du an:

docker compose down


5. TEP TAI LIEU LIEN QUAN
-------------------------
- 03_Deployment/README.md
- 03_Deployment/HUONG_DAN_CAI_DAT.md
- 03_Deployment/YEU_CAU_MOI_TRUONG.md
- 03_Deployment/.env.example
- 03_Deployment/docker-compose.yml
- 03_Deployment/config/application-example.properties
- 03_Deployment/config/frontend-env.example
