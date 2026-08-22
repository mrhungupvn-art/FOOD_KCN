# COM11 SHOP LIVE V1

This branch is a new product built from the existing `com11` Android app. The original `main` branch is intentionally untouched.

## Current V1 scope

- Reuse existing COM11H account/token through `AccountSync`.
- New Shop Live launcher and navigation shell.
- Shop/product data models.
- Product listing and local cart interaction.
- Order domain schema for demo backend.
- Seller entry point and Live start/stop API contract.
- Live room listing, playback URL support, chat API hook, and pinned-product API hook.
- Demo MySQL schema under `backend/demo/`.

## API actions introduced by the mobile facade

`shops`, `products`, `cart`, `orders`, `live_rooms`, `live_room`, `live_message`, `live_pin_product`, `live_start`, `live_stop`.

These are an API contract for the V1 branch; production backend implementation must validate the authenticated user, seller/shop ownership, product ownership, stock, and live-room state before changing data.

## Live infrastructure rule

Do not store video bytes in MySQL or on the 2 GB web host. The API stores metadata and playback URLs only. A separate Live Server/CDN can later provide HLS/WebRTC playback without changing the Shop/Order data model.

## Safety

- `main` remains the rollback point.
- Demo SQL is not production migration code.
- Production deployment requires database backup and a reviewed migration.
- Seller actions must be authorization-checked server-side; hiding UI controls is not security.
