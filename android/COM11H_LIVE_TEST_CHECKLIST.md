# COM11H LIVE - Test Checklist V1

Branch: `com11-shop-live-v1`

## A. Customer
- [ ] App opens without crash.
- [ ] Existing COM11H login is preserved.
- [ ] LIVE tab opens.
- [ ] Active LIVE rooms are listed.
- [ ] Opening a LIVE shows the stream area.
- [ ] Pinned products appear before the normal shop menu.
- [ ] Pinned order is preserved (1 -> 5).
- [ ] Normal products remain visible below pinned products.
- [ ] Pinned products are not duplicated in the normal list.
- [ ] Out-of-stock products cannot be added to cart.
- [ ] Customer can add a product from the LIVE to cart.
- [ ] Customer can send LIVE chat.
- [ ] Customer can proceed to the existing COM11H order flow.

## B. Seller / Partner
- [ ] Existing Partner login is recognized as Seller.
- [ ] Non-Partner account cannot open Seller controls.
- [ ] Partner is resolved to exactly one shop.
- [ ] Seller cannot choose another shop.
- [ ] Seller product list contains only products of the resolved shop.
- [ ] Seller can start LIVE for the resolved shop.
- [ ] Seller can stop only their own LIVE.
- [ ] Seller can pin only products belonging to their shop.
- [ ] Maximum pinned products is 5.
- [ ] Pinned order can be changed without changing the shop menu order.

## C. Security / backend acceptance
- [ ] Backend ignores/rejects a forged shop_id.
- [ ] Backend rejects product_id belonging to another shop.
- [ ] Backend rejects live_room_id belonging to another seller.
- [ ] Backend rejects pin/unpin/reorder requests from non-sellers.
- [ ] Backend validates active product and available stock before purchase.
- [ ] Existing customer order APIs remain compatible.

## D. Release gate

Do **not** merge into `main` until A + B + C pass on a real device and the production API is connected.

The Android UI rules are convenience checks only; authorization must remain server-side.
