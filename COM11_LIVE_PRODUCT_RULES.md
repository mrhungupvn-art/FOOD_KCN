# COM11H LIVE — Product & Shop Rules (V1)

## Core ownership

`1 Partner account = 1 COM11H shop`.

The Android Seller UI must never ask the Seller to choose another shop. After login, `seller_context` resolves the Partner's shop. The backend remains the authority for this rule.

## Products

A Seller can only:

- see products belonging to their own shop;
- create/edit the name, price and stock of their own products through the authorized Partner flow;
- use their own products in LIVE;
- pin/unpin/reorder their own products.

A Seller must never be able to use another shop's product by changing an ID in an API request.

Every protected action must validate:

`authenticated_user -> partner_id -> shop_id -> product.shop_id`

## Customer LIVE screen

When a shop is LIVE, the room exposes two product layers:

1. **SẢN PHẨM NỔI BẬT / ĐANG GHIM** — pinned products at the top.
2. **TẤT CẢ SẢN PHẨM CỦA SHOP** — the complete active product list below.

Pinned products are not removed from the shop's normal product list. They are promoted to the top while LIVE.

## Pin priority

The pinned list is ordered.

- Position 1 = highest priority.
- Maximum recommended V1 pinned products = 5.
- The Seller can pin, unpin and reorder products.
- When a new product is selected as the current highlight, it may be moved to position 1.

The API represents this with `pinned_product_ids`, ordered from highest to lowest priority.

## LIVE purchase behavior

A customer can add a product directly from the highlighted area or from the full shop product list while remaining in the LIVE room.

The order must use the existing COM11H order/business rules. LIVE is only the sales channel; it is not a second order database.

## Backend actions required

- `seller_context`
- `live_rooms`
- `live_room`
- `products?shop_id=<shop_id>`
- `live_start`
- `live_stop`
- `live_pin_product`
- `live_unpin_product`
- `live_reorder_pinned`
- `live_message`
- existing COM11H order/cart actions

Voucher and Flash Sale actions are already reserved in the Android API facade, but must not be enabled in production until their server-side authorization and order-price validation are implemented.

## Security

Never trust `shop_id`, `partner_id`, or `product_id` supplied by the Android client. Resolve ownership from the authenticated COM11H token and server-side relationships.

The Android app may hide controls, but backend authorization is mandatory.
