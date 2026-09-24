# Fashion Deployment

Thu muc nay chua tai lieu va tep cau hinh mau de cai dat, chay va trien khai he thong Fashion.

## Noi dung

- `HUONG_DAN_CAI_DAT.md`: Huong dan cai dat va chay he thong theo tung buoc.
- `YEU_CAU_MOI_TRUONG.md`: Danh sach phan mem, cong cu va cong dich vu can co.
- `docker-compose.yml`: Tep Docker Compose dung de khoi dong PostgreSQL, Redis, Elasticsearch va RabbitMQ.
- `.env.example`: Cac bien moi truong mau, khong chua thong tin bi mat that.
- `config/application-example.properties`: Cau hinh backend Spring Boot mau.
- `config/frontend-env.example`: Cau hinh frontend Vite mau.

## Trinh tu trien khai nhanh

1. Cai dat cac cong cu trong `YEU_CAU_MOI_TRUONG.md`.
2. Sao chep `.env.example` thanh `.env` neu can cau hinh bang bien moi truong.
3. Khoi dong dich vu phu tro bang Docker Compose.
4. Chay backend Spring Boot.
5. Chay frontend Vite.
6. Chay kiem thu bang `mvn test`.

Khong dua mat khau that, JWT secret that, VNPay secret that hoac thong tin nhay cam vao thu muc nop.
