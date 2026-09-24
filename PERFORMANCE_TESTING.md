# Giai doan 6 - Performance testing

Chi chay JMeter sau khi functional va security regression da on dinh. Khong dung kich ban nay de day tai cuc lon neu NFR cua bao cao khong yeu cau.

## Test plan

File JMeter:

```text
performance/fashion-performance.jmx
```

Cay test plan:

```text
Test Plan
└── Thread Group
    ├── HTTP Request Defaults
    ├── HTTP Header Manager
    ├── Login
    ├── Products
    ├── Search
    ├── View Results Tree
    └── Summary Report
```

Endpoint mac dinh:

- `POST /api/auth/login`
- `GET /api/products`
- `GET /api/search?keyword=ao&page=0&size_param=10`

Tai khoan mac dinh lay theo `RoleInitializer`:

- Email: `customer@fashion.local`
- Password: `Customer@123`

## Chuan bi

1. Khoi dong database va backend Spring Boot o `http://localhost:8080`.
2. Neu can test thong qua frontend thi khoi dong them Vite, nhung JMeter plan nay ban vao backend API de do on dinh response time cua server:

```powershell
cd frontend
npm run dev
```

3. Mo JMeter GUI de kiem tra plan:

```powershell
jmeter -t performance/fashion-performance.jmx
```

## Chay nhe truoc

Nen chay non-GUI de ket qua it bi anh huong boi giao dien JMeter.

### 10 users

```powershell
jmeter -n -t performance/fashion-performance.jmx -l performance/results-10u.jtl -e -o performance/report-10u -Jthreads=10 -Jramp=10 -Jloops=5
```

### 50 users

```powershell
jmeter -n -t performance/fashion-performance.jmx -l performance/results-50u.jtl -e -o performance/report-50u -Jthreads=50 -Jramp=50 -Jloops=5
```

### 100 users

```powershell
jmeter -n -t performance/fashion-performance.jmx -l performance/results-100u.jtl -e -o performance/report-100u -Jthreads=100 -Jramp=100 -Jloops=5
```

Neu muon doi host/port:

```powershell
jmeter -n -t performance/fashion-performance.jmx -l performance/results-10u.jtl -Jhost=localhost -Jport=8080 -Jthreads=10 -Jramp=10 -Jloops=5
```

## Chi so can thu thap

Ghi lai cac cot sau tu Summary Report hoac HTML Dashboard:

| Run | Samples | Average | Median | P90 | P95 | Min | Max | Throughput | Error % |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 10 users / ramp 10s / loop 5 |  |  |  |  |  |  |  |  |  |
| 50 users / ramp 50s / loop 5 |  |  |  |  |  |  |  |  |  |
| 100 users / ramp 100s / loop 5 |  |  |  |  |  |  |  |  |  |

Quan trong nhat khi viet bao cao:

- Response Time: `Average`, `Median`, `P90`, `P95`
- Throughput
- Error Rate

## Cach doc ket qua

- PASS tot: `Error % = 0%` hoac rat gan 0%, P95 nam trong NFR cua bao cao.
- Can dieu tra: error tang khi len 50/100 users, P95 tang dot bien, hoac throughput khong tang khi tang users.
- Khong ket luan bug neu chi co mot spike don le. Can xem response code, backend log va co lap lai duoc khong.

## Mau nhan xet bao cao

```text
Voi muc 10 users, he thong xu ly on dinh, Error Rate dat ...%, P95 dat ... ms va Throughput dat ... req/s.
Khi tang len 50 users, P95 tang len ... ms, Throughput dat ... req/s, Error Rate ...%.
Khi tang len 100 users, he thong ... . Ket qua nay cho thay gioi han/chua thay gioi han trong pham vi NFR cua de tai.
```
