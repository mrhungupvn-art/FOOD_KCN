# FOOD KCN 1.8.0 — XU & Chăm sóc khách hàng

## Cơ chế XU
- Xem một sản phẩm đủ 30 giây: +10 XU.
- Xem đủ 10 sản phẩm khác nhau: thưởng thêm 100 XU.
- Tối đa 200 XU/giờ.
- Tối đa 2.000 XU/ngày.
- XU hiển thị trong Ví XU và có màn hình đổi thử voucher.

## Lưu ý quan trọng
Toàn bộ app (HomeActivity + MainActivity) hiện đọc/ghi Xu qua `XuApi.kt` (gọi
thẳng server), không còn màn nào dùng bộ đếm cục bộ trên máy nữa — `XuStore.kt`
(bản demo dùng SharedPreferences, không chống gian lận) đã được gỡ bỏ khỏi
project. Số Xu hiển thị ở bong bóng nhắc nhở trên Trang chủ, màn Chi tiết món
và Ví XU đều là số THẬT từ server, luôn khớp nhau.

Để chạy production, backend `api/index.php` cần có đầy đủ 2 action sau (theo
đúng cấu trúc mà `XuApi.kt`/`MainActivity.kt` đang gọi: `xu_wallet`,
`xu_start_view`, `xu_complete_view`, `xu_history`, `xu_rewards`, `xu_redeem`):

### GET `action=xu_balance`
Authorization: Bearer TOKEN

Response mẫu:
```json
{"ok":true,"data":{"xu":1280,"day_xu":650,"hour_xu":120,"watched_today":8}}
```

### POST `action=xu_watch`
Body:
```json
{"product_id":123,"watch_seconds":30}
```

Server phải kiểm tra token, sản phẩm, thời gian xem, sản phẩm đã nhận XU, giới hạn 200 XU/giờ, 2.000 XU/ngày và tự cộng thưởng 100 XU khi đủ 10 sản phẩm.

## Hướng production
Không tin số giây do Android tự gửi. Server nên tạo `watch_session_id` khi mở chi tiết sản phẩm và xác nhận phiên xem trước khi cộng XU.
