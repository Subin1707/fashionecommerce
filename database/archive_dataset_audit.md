# Audit `E:\archive.zip`

## Kết quả kiểm tra

- Archive chứa bộ dữ liệu ảnh sản phẩm thời trang Myntra (dấu hiệu: `styles.csv`, `images/<id>.jpg`, metadata `masterCategory`, `articleType`, `productDisplayName`).
- Có 44.446 dòng metadata và 44.446 ID riêng; hai thư mục ảnh (`images/` và `myntradataset/images/`) là hai bản giống hệt nhau, mỗi bản có 44.441 ảnh. 5 ID metadata không có ảnh trong archive.
- Ảnh mẫu là JPEG 60×80 px. Kích thước này quá nhỏ để dùng làm ảnh sản phẩm hiển thị rõ trên catalog; phóng to không khôi phục chi tiết.
- ZIP không kèm README, file license hoặc attribution; CSV chỉ có metadata sản phẩm.
- Metadata gồm các trường giới tính, nhóm danh mục, loại mặt hàng, màu, mùa, năm, mục đích sử dụng và tên hiển thị. Nó mô tả hàng hóa của bên thứ ba, không có giá/stock/variant đủ tin cậy để tạo listing trong Fashion Shop.

## Quyết định tích hợp

Kaggle hiện hiển thị MIT cho dataset bản nhỏ `Fashion Product Images (Small)`, khớp với thumbnail 60×80 trong archive này: https://www.kaggle.com/paramaggarwal/fashion-product-images-small. Theo yêu cầu của chủ dự án, đã tạo một subset development/demo có attribution và tách biệt khỏi catalog gốc.

Giới hạn quyền vẫn cần lưu ý: Myntra xác nhận ảnh và văn bản trên platform được bảo vệ và hạn chế sao chép/phân phối nếu chưa có chấp thuận bằng văn bản: https://www.myntraglobal.com/pages/terms-of-use. Kaggle cũng nói license listing là thông tin do người đăng cung cấp, người dùng tự kiểm tra điều khoản. Do đó seed này phù hợp làm fixture demo theo yêu cầu chủ dự án; chưa có căn cứ xác nhận quyền thương mại của từng ảnh gốc. Không xem đây là bằng chứng rằng ảnh đã được cleared cho storefront sản phẩm thật.

Subset seed giữ attribution, ảnh gốc và tên mẫu; giá, tồn kho, lựa chọn size do seed tạo là dữ liệu demo. Để dùng storefront thương mại, cần xác nhận quyền ảnh và nội dung hoặc thay bằng ảnh tự chụp/ảnh stock được cấp phép và metadata do Fashion Shop sở hữu.

## Đối chiếu category ở mức khái niệm

| Nhóm trong archive | Gợi ý nhóm taxonomy Fashion Shop |
|---|---|
| Apparel / Topwear | Áo, áo thun, áo sơ mi, áo khoác, áo len |
| Apparel / Bottomwear | Quần, quần jean, quần short, chân váy |
| Apparel / Saree, Ethnic, Kurtas | Trang phục truyền thống / Việt phục chỉ khi mặt hàng thực sự là trang phục Việt |
| Footwear | Giày, sneaker, boots, sandal, dép |
| Accessories / Bags, Wallets, Belts, Watches, Eyewear, Jewellery | Túi, ví, thắt lưng, đồng hồ, kính, trang sức |
| Innerwear, Personal Care | Chỉ ánh xạ nếu cửa hàng có danh mục và nghiệp vụ tương ứng |

Đây là gợi ý phân loại, không phải lệnh tự tạo danh mục hoặc sản phẩm. Nguồn dữ liệu tập trung vào thị trường/nhãn hàng Ấn Độ; không nên gán các dòng `Ethnic`, `Saree` hoặc `Kurtas` thành Việt phục.

## Subset đã nạp cho môi trường demo

- `database/seed_myntra_demo.sql` là seed idempotent riêng; `database/seed.sql` cũng có khối được đánh dấu để nạp lại cùng subset khi khởi tạo database.
- Script tạo dữ liệu: `database/build_myntra_demo_seed.py`.
- Manifest: `frontend/public/assets/products/myntra-sample/manifest.json`; attribution: `frontend/public/assets/products/myntra-sample/ATTRIBUTION.md`.
- Đã nạp vào PostgreSQL local `fashion` trên `127.0.0.1:5432`: 1.000 sản phẩm, 8 danh mục (125 sản phẩm/danh mục), 1.000 ảnh liên kết; 0 bản ghi thiếu ảnh, 0 tên trùng, 0 file ảnh trùng SHA-256.
- Giá, stock, và size là dữ liệu demo tổng hợp; không đại diện cho giá hay biến thể gốc.
