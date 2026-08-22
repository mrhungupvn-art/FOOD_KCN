# COM11H Seller / LIVE Production Contract

## 1. Identity rule

The Android app has only two user roles:

- `CUSTOMER`: normal COM11H customer account.
- `SELLER`: an existing COM11H web Partner account.

There is **no separate seller account table and no second seller password** in the mobile app.

The mobile app sends the same Bearer token already issued by `com11h.com/api/index.php`. The production API resolves that token to the existing web user/partner record.

## 2. Seller context API

### Request

`GET /api/index.php?action=seller_context`

Authenticated with:

`Authorization: Bearer <existing_com11h_token>`

### Non-seller response

```json
{
  "ok": true,
  "data": {
    "is_seller": false,
    "partner_id": null,
    "display_name": null,
    "shops": []
  }
}
```

### Seller response

```json
{
  "ok": true,
  "data": {
    "is_seller": true,
    "partner_id": 123,
    "display_name": "Tên đối tác",
    "shops": [
      {
        "id": 45,
        "name": "Tên cửa hàng",
        "logo_url": "https://...",
        "status": "active"
      }
    ]
  }
}
```

## 3. Authorization requirements

The API, not Android UI, must enforce:

- token belongs to an authenticated account;
- account is an approved Partner/Seller;
- seller owns the selected shop;
- seller owns/manages the products being pinned;
- seller can only start/stop their own live room;
- inactive/suspended shops cannot start LIVE;
- stock and order permissions remain governed by the existing COM11H business logic.

## 4. LIVE actions

Existing mobile facade actions remain:

- `live_rooms` — public/authorized list of active rooms;
- `live_room` — room detail and playback URL;
- `live_start` — create/start a room for an owned shop;
- `live_stop` — stop an owned room;
- `live_message` — send chat message;
- `live_pin_product` — pin an owned product in an owned live room.

`live_start` must receive a real `shop_id`; the app must never use a hard-coded or placeholder shop ID.

## 5. Video storage rule

Do not store video bytes in the COM11H MySQL database or the existing 2 GB web host.

The COM11H API stores live-room metadata only:

- room ID
- shop/partner ID
- title
- status
- cover URL
- playback URL
- viewer count
- timestamps
- pinned product

A dedicated Live Server/CDN should provide HLS/WebRTC media.

## 6. Compatibility rule

Existing customer login, menu, ordering and account behavior must remain unchanged. LIVE is an extension of the existing COM11H platform, not a replacement backend.

## 7. Deployment order

1. Back up production database.
2. Add the production API actions/authorization to the existing COM11H API.
3. Verify `seller_context` with a real existing Partner account.
4. Verify `live_start`/`live_stop` ownership checks.
5. Connect the Live Server/CDN playback URL.
6. Test customer watch/chat/purchase flow.
7. Test seller LIVE flow.
8. Only then release the Android build.
