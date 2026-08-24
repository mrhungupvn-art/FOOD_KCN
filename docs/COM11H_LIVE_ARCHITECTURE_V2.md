# COM11H LIVE V2 — Customer App vs Partner/Seller App

## Quyết định kiến trúc

COM11H LIVE không dùng chung màn hình LIVE cho khách hàng và đối tác.

- **Customer App**: chỉ xem LIVE, chat, xem sản phẩm của shop đang LIVE và mua hàng.
- **Partner/Seller App**: dùng chính tài khoản Đối tác của COM11H web để đăng nhập, phát LIVE và quản lý phiên LIVE.
- **Web Partner Center**: là nơi quản lý dữ liệu/quyền của đối tác: sản phẩm, giá, tồn kho và cấu hình/quản lý LIVE.
- **Backend**: là nguồn sự thật về quyền `partner -> shop/category -> product -> live`.

## Tài khoản

Không tạo một tài khoản seller phụ cho mô hình chính thức.

`partner_accounts` là tài khoản Seller của app đối tác.

Token app đối tác (`partner_api_tokens`) chỉ là token API riêng cho thiết bị; nó không tạo ra một danh tính Seller thứ hai và không thay thế session web.

## Luồng dữ liệu

```text
COM11H Web
  Partner Account
      |
      +--> Product / price / stock
      |
      +--> Partner LIVE management
      |
      v
Backend API
      |
      +-------------------+
      |                   |
Customer App         Partner/Seller App
      |                   |
  View LIVE          Start/Stop LIVE
  Chat               Camera/stream
  Products           Pin products
  Cart/Order         Voucher/Flash Sale
      |                   |
      +---------+---------+
                v
          Correct Partner/Shop
                |
             Orders
```

## Customer App

Customer LIVE screen must not expose Seller controls such as `Bắt đầu LIVE`, Seller Center, product administration, or seller-only actions.

LIVE room order:

1. Video/live playback
2. Seller/shop identity
3. Pinned products (maximum 5, ordered by seller)
4. All eligible products of the same shop
5. Chat
6. Buy/add-to-cart actions

Pinned products are presentation-priority only; checkout and inventory rules remain the normal COM11H order rules.

## Partner/Seller App

The partner app uses the existing partner username/password from COM11H web.

After authentication the backend determines the partner context. The app must never let the partner type/select an arbitrary `partner_id` or shop belonging to another account.

Seller flow:

1. Partner login
2. Load partner/shop context
3. Show only the partner's eligible products
4. Create LIVE
5. Select/pin products from that same product set
6. Start LIVE
7. Manage pinned order/chat/voucher/flash-sale features
8. End LIVE
9. Show LIVE statistics/order summary

## Web Partner Center gap

The production web source already contains partner accounts and API LIVE actions, but the Partner UI does not yet present a clear first-class `LIVE` management area alongside the existing product management.

The next backend/web task is therefore **not** to invent another Seller account system. It is to add a clear Partner Center LIVE module backed by the existing partner identity.

Recommended navigation:

- Tổng quan
- Món ăn / Sản phẩm
- Giá & tồn kho
- Đơn hàng (according to existing permissions)
- **LIVE bán hàng**
  - Tạo phiên LIVE
  - Sản phẩm LIVE
  - Sản phẩm ghim / thứ tự
  - Voucher
  - Flash Sale
  - Lịch sử LIVE
  - Thống kê

## Legacy seller tables/code

The web source contains legacy `partner_sellers` / `seller_*` administration code. The official architecture must not use this as a second seller identity if `partner_accounts` is already the source of truth.

Before production merge, audit and either remove, isolate, or explicitly mark the legacy seller path so the same partner cannot accidentally receive two independent Seller identities.

## Security rules

Backend must verify ownership for every seller-only operation:

- LIVE belongs to authenticated partner.
- Product belongs to the same partner/shop/category allowed to that partner.
- Pinned product belongs to the LIVE's partner.
- Order remains subject to the normal COM11H checkout/inventory rules.
- Client-supplied `partner_id`, `shop_id`, `food_id`, or `live_id` must never be trusted without server-side ownership checks.

## Release rule

This architecture is implemented on `com11-shop-live-v1` first. Do not merge to `main` until the Customer App and Partner/Seller App flows are separately testable and the web Partner LIVE management is connected to the same backend identity.
