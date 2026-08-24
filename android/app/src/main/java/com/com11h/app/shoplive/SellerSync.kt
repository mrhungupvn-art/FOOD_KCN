package com.com11h.app.shoplive

import android.content.Context
import com.com11h.app.AccountSync
import org.json.JSONArray

/**
 * Identity/authorization bridge for the existing COM11H partner account.
 *
 * A Seller is the existing COM11H web Partner. The app never creates a second
 * seller identity and never lets the seller choose an arbitrary shop.
 */
class SellerSync(context: Context) {
    private val account = AccountSync(context.applicationContext)

    data class SellerContext(
        val isSeller: Boolean,
        val partnerId: Long?,
        val displayName: String?,
        val shop: SellerShop?
    )

    data class SellerShop(
        val id: Long,
        val name: String,
        val logoUrl: String?,
        val status: String
    )

    fun context(): SellerContext {
        if (!account.isLoggedIn()) return SellerContext(false, null, null, null)

        val root = account.request("seller_context")
        val data = root.optJSONObject("data") ?: root
        val isSeller = data.optBoolean("is_seller", false)
        if (!root.optBoolean("ok", false) || !isSeller) {
            return SellerContext(false, null, null, null)
        }

        // Production rule: one Partner owns one shop. If the API returns more
        // than one record, the app does not silently let the Seller switch
        // shops. Backend must resolve the canonical shop for this Partner.
        val shopsArray = data.optJSONArray("shops") ?: JSONArray()
        val shop = if (shopsArray.length() == 1) parseShop(shopsArray.optJSONObject(0)) else null

        return SellerContext(
            isSeller = shop != null,
            partnerId = data.optLong("partner_id").takeIf { it > 0 },
            displayName = data.optString("display_name").ifBlank { null },
            shop = shop
        )
    }

    private fun parseShop(shop: org.json.JSONObject?): SellerShop? {
        if (shop == null) return null
        val id = shop.optLong("id")
        if (id <= 0) return null
        return SellerShop(
            id = id,
            name = shop.optString("name", "Cửa hàng COM11H"),
            logoUrl = shop.optString("logo_url").ifBlank { null },
            status = shop.optString("status", "active")
        )
    }
}
