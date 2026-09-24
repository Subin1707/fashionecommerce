# Hướng dẫn kiểm thử

## JUnit backend

Yêu cầu: JDK 21 và kết nối Maven Central khi dependency chưa có trong cache.

Chạy toàn bộ test ở thư mục gốc:

```powershell
.\mvnw.cmd test
```

Chạy một lớp test:

```powershell
.\mvnw.cmd "-Dtest=VoucherServiceUnitTest" test
```

Các test JUnit dùng profile `test` và H2 trong bộ nhớ theo cấu hình
`src/test/resources/application-test.properties`, nên không cần PostgreSQL cho nhóm unit/integration test này.
Báo cáo Maven nằm trong `target/surefire-reports`.

## Playwright E2E functional testing

Mục tiêu giai đoạn 4 là chứng minh functional testing cho luồng:

```text
PostgreSQL -> Spring Boot Backend localhost:8080 -> React Frontend localhost:5173
```

Trước khi chạy E2E đầy đủ, cần bật PostgreSQL và backend Spring Boot. Frontend Vite sẽ được Playwright tự khởi động theo `frontend/playwright.config.js` nếu chưa có server ở `localhost:5173`.

Trên Windows PowerShell, dùng `npx.cmd` để tránh lỗi execution policy của `npx.ps1`:

```powershell
cd frontend
npx.cmd playwright --version
npx.cmd playwright test
npx.cmd playwright show-report
```

Nếu muốn chỉ xem log console gọn:

```powershell
npx.cmd playwright test --reporter=list
```

## Cấu trúc Playwright

```text
frontend/tests/
├── auth.spec.js
├── product.spec.js
├── search.spec.js
├── cart.spec.js
├── voucher.spec.js
├── checkout.spec.js
├── payment.spec.js
├── order.spec.js
├── shipping.spec.js
├── review.spec.js
├── wishlist.spec.js
├── smart-size.spec.js
└── admin.spec.js
```

Bộ E2E hiện có 45 functional cases:

- TC-01..TC-05: authentication, đăng ký, trùng email, đăng nhập, sai mật khẩu, validate form.
- TC-06..TC-13: product, search, detail, variant color/size, stock theo variant, quantity không vượt tồn.
- TC-14..TC-16: search/filter/sort/reset.
- TC-17..TC-20: cart add/increase/decrease/remove/empty state.
- TC-21..TC-22: voucher input và mã không hợp lệ.
- TC-23..TC-25: checkout form, validate dữ liệu, COD.
- TC-26..TC-27: payment method và payment return route.
- TC-28..TC-30: order list/profile/order detail missing id.
- TC-31..TC-32: shipping workflow và shipment context.
- TC-33..TC-35: review list, protected route, eligible/empty state.
- TC-36..TC-38: wishlist load/add/remove.
- TC-39..TC-41: smart size input hợp lệ, input biên, style recommendation.
- TC-42..TC-45: admin dashboard/products/inventory/orders.

Admin cases được skip nếu chưa cấu hình tài khoản admin. Để chạy nhóm này:

```powershell
$env:E2E_ADMIN_EMAIL="admin@fashion.local"
$env:E2E_ADMIN_PASSWORD="<admin-password>"
npx.cmd playwright test tests/admin.spec.js
```

Lần kiểm tra gần nhất trong môi trường này:

- `npx.cmd playwright --version`: `Version 1.63.0`
- `npx.cmd playwright test --reporter=list`: 41 passed, 4 skipped admin cases do chưa set `E2E_ADMIN_PASSWORD`.
- Lưu ý: runner đã in đủ các dòng `ok` cho 41 case và 4 dòng skip, nhưng tiến trình bị kẹt ở bước cleanup/report nên đã được ngắt bằng `Ctrl+C`.
