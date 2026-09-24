# Giai doan 5 - Security testing

Muc tieu: kiem tra cac yeu cau SEC-01..SEC-16 sau khi functional testing da on dinh.

## OWASP ZAP Automated Scan

1. Khoi dong backend Spring Boot o `http://localhost:8080`.
2. Khoi dong frontend:

```powershell
cd frontend
npm run dev
```

3. Mo OWASP ZAP bang ung dung desktop.
4. Chon `Automated Scan`.
5. Nhap URL:

```text
http://localhost:5173
```

6. Chay scan va ghi nhan tong so finding theo nhom `High`, `Medium`, `Low`, `Informational`.
7. Khong mac dinh coi moi finding cua ZAP la bug that. Can mo tung alert, kiem tra request/response, doi chieu code va danh dau `True positive`, `False positive`, hoac `Accepted risk`.

Trong moi truong terminal hien tai chua co lenh `zaproxy`/`zap.bat` trong PATH, nen ZAP can duoc mo bang desktop app.

## JUnit security regression

Chay rieng cac case security tu dong:

```powershell
.\mvnw.cmd "-Dtest=SecurityIntegrationTest" test
```

Ket qua gan nhat:

- `SecurityIntegrationTest`: 7 passed, 0 failed, 0 skipped.

## Security checklist

| ID | Hang muc | Ket qua / cach xac minh |
| --- | --- | --- |
| SEC-01 | HTTPS | Moi truong dev dung `http://localhost:5173`; production can bat HTTPS/TLS o reverse proxy hoac hosting. Backend da cau hinh HSTS header khi truy cap qua HTTPS. |
| SEC-02 | Password Hash | PASS tu dong: `sec02PasswordIsStoredAsBCryptHash` xac nhan password khong luu plain text, hash bat dau bang `$2`, va BCrypt verify duoc. |
| SEC-03 | SQL Injection | Can xac minh qua ZAP va test input doc hai. Code dang dung Spring Data JPA/repository parameter binding, khong thay ghep SQL truc tiep trong luong chinh. |
| SEC-04 | XSS | Can xac minh qua ZAP va UI manual. React mac dinh escape text render; can kiem tra cac truong review/search/profile voi payload `<script>alert(1)</script>`. |
| SEC-05 | CSP | PASS tu dong: `sec05AddsContentSecurityPolicyHeader` kiem tra header `Content-Security-Policy` co `default-src 'self'` va `frame-ancestors 'none'`. |
| SEC-06 | CSRF / co che tuong ung | PASS tu dong: `sec06CsrfUsesStatelessJwtBearerTokenInsteadOfSynchronizerToken` xac nhan API state-changing dung Bearer JWT stateless, khong phu thuoc session cookie CSRF token. |
| SEC-07 | RBAC | PASS tu dong: `sec07CustomerTokenCannotAccessAdminEndpoint` xac nhan CUSTOMER token goi `POST /api/admin/products` bi `403 Forbidden`. |
| SEC-08 | IDOR | PASS tu dong: `sec08CustomerCannotReadAnotherCustomersOrder` xac nhan Customer A truy cap order cua Customer B khong lay duoc du lieu. |
| SEC-09 | Invalid JWT | PASS tu dong: `sec09InvalidJwtCannotAccessProtectedEndpoint` xac nhan token sai khong truy cap duoc `/api/orders`. |
| SEC-10 | Expired JWT | PASS tu dong: `sec10ExpiredJwtCannotAccessProtectedEndpoint` xac nhan token het han khong truy cap duoc `/api/orders`. |
| SEC-11 | Login Rate Limit | Chua thay co rate limiting trong code hien tai. Nen bo sung bucket/token limit theo email/IP cho `/api/auth/login`, hoac cau hinh reverse proxy/API gateway. |
| SEC-12 | Upload file type | Can kiem tra khi co endpoint upload that. Review media hien tai dang nhan data URL; nen whitelist MIME/type neu cho upload file. |
| SEC-13 | Upload file size | Can kiem tra khi co endpoint upload that. Nen cau hinh max request/file size va validate kich thuoc base64/file. |
| SEC-14 | Transaction logging | Co lich su trang thai don hang `OrderStatusHistory` cho thay doi trang thai. Nen log them actor/userId, timestamp va hanh dong thanh toan neu can audit day du. |
| SEC-15 | Payment sensitive information | VNPay/payment flow khong nen log secret/card data. Can verify log runtime va response khong tra ve thong tin nhay cam. |
| SEC-16 | Backup/Restore | Co `backup.sql` trong repo. Can test restore vao database moi va ghi ket qua restore thanh cong/thoi gian restore. |

## Case authorization bat buoc

CUSTOMER token bi chan khoi admin API:

```text
CUSTOMER token -> POST /api/admin/products -> 403 Forbidden
```

IDOR don hang:

```text
Customer A token -> GET /api/orders/{orderId cua Customer B} -> khong tra ve du lieu don hang
```

Trong code hien tai case IDOR tra ve `400 Bad Request` voi thong bao don hang khong ton tai theo ngu canh user, chap nhan duoc ve mat khong lo du lieu. Neu muon dung dung tieu chi cua thay hon, co the doi handler/service sang `404 Not Found`.
