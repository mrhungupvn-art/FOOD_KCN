package com.com11h.app.shoplive

import android.content.Context
import com.com11h.app.AccountSync
import org.json.JSONArray
import org.json.JSONObject

/** COM11H LIVE API facade. The existing COM11H token remains the only identity. */
class ShopLiveApi(context: Context) {
    private val account = AccountSync(context.applicationContext)
    private val seller = SellerSync(context.applicationContext)

    fun isLoggedIn() = account.isLoggedIn()
    fun token() = account.token()
    fun logout() = account.logout()

    fun shops(): JSONObject = account.request("shops")
    fun products(shopId: Long? = null, q: String? = null): JSONObject = account.request("products", query = buildMap {
        shopId?.let { put("shop_id", it.toString()) }
        q?.takeIf { it.isNotBlank() }?.let { put("q", it) }
    })
    fun cart(): JSONObject = account.request("cart")
    fun orders(): JSONObject = account.request("orders")
    fun liveRooms(): JSONObject = account.request("live_rooms")
    fun liveRoom(id: Long): JSONObject = account.request("live_room", query = mapOf("id" to id.toString()))
    fun sellerContext(): JSONObject = account.request("seller_context")

    fun sendLiveMessage(roomId: Long, message: String): JSONObject = account.request(
        "live_message", method = "POST",
        body = JSONObject().put("room_id", roomId).put("message", message).toString()
    )

    /** Backend must verify that product.shop_id == seller's shop_id and room.shop_id == seller's shop_id. */
    fun pinLiveProduct(roomId: Long, productId: Long): JSONObject = account.request(
        "live_pin_product", method = "POST",
        body = JSONObject().put("room_id", roomId).put("product_id", productId).toString()
    )

    fun unpinLiveProduct(roomId: Long, productId: Long): JSONObject = account.request(
        "live_unpin_product", method = "POST",
        body = JSONObject().put("room_id", roomId).put("product_id", productId).toString()
    )

    /** Ordered IDs: first item is shown first in the highlighted/pinned area. */
    fun reorderPinnedProducts(roomId: Long, productIds: List<Long>): JSONObject = account.request(
        "live_reorder_pinned", method = "POST",
        body = JSONObject().put("room_id", roomId).put("product_ids", JSONArray(productIds)).toString()
    )

    fun createLiveVoucher(roomId: Long, amountVnd: Long, minOrderVnd: Long, quantity: Int): JSONObject = account.request(
        "live_voucher_create", method = "POST",
        body = JSONObject().put("room_id", roomId).put("amount_vnd", amountVnd).put("min_order_vnd", minOrderVnd).put("quantity", quantity).toString()
    )

    fun createFlashSale(roomId: Long, productId: Long, priceVnd: Long, quantity: Int, durationSeconds: Int): JSONObject = account.request(
        "live_flash_sale_create", method = "POST",
        body = JSONObject().put("room_id", roomId).put("product_id", productId).put("price_vnd", priceVnd).put("quantity", quantity).put("duration_seconds", durationSeconds).toString()
    )

    /**
     * Seller does not choose shop_id in the UI. The app resolves the single
     * shop owned by the authenticated Partner and sends that canonical ID.
     */
    fun startLive(title: String): JSONObject {
        val ctx = seller.context()
        val shopId = ctx.shop?.id ?: return JSONObject()
            .put("ok", false)
            .put("message", "Tài khoản Seller chưa được gắn với một cửa hàng hợp lệ.")
        if (ctx.shop.status != "active") return JSONObject()
            .put("ok", false)
            .put("message", "Cửa hàng hiện không thể bắt đầu LIVE.")
        return account.request(
            "live_start", method = "POST",
            body = JSONObject().put("shop_id", shopId).put("title", title).toString()
        )
    }

    fun stopLive(roomId: Long): JSONObject = account.request(
        "live_stop", method = "POST",
        body = JSONObject().put("room_id", roomId).toString()
    )

    companion object {
        fun jsonArray(obj: JSONObject, key: String): JSONArray = obj.optJSONArray(key)
            ?: obj.optJSONObject("data")?.optJSONArray(key)
            ?: JSONArray()
    }
}
