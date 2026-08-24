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
        val shop: SellerShop?,
        /** Compatibility accessor: contains zero or one shop only. */
        val shops: List<SellerShop>
    )

    data class SellerShop(
        val id: Long,
        val name: String,
        val logoUrl: String?,
        val status: String
    )

    fun context(): SellerContext {
        if (!account.isLoggedIn()) return emptyContext()

        val root = account.request("seller_context")
        val data = root.optJSONObject("data") ?: root
        val isSeller = data.optBoolean("is_seller", false)
        if (!root.optBoolean("ok", false) || !isSeller) return emptyContext()

        // Production rule: one Partner owns exactly one shop. If the API returns
        // more than one record, the app refuses to choose one silently.
        val shopsArray = data.optJSONArray("shops") ?: JSONArray()
        val shop = if (shopsArray.length() == 1) parseShop(shopsArray.optJSONObject(0)) else null
        if (shop == null) {
            return SellerContext(
                isSeller = false,
                partnerId = data.optLong("partner_id").takeIf { it > 0 },
                displayName = data.optString("display_name").ifBlank { null },
                shop = null,
                shops = emptyList()
            )
        }

        return SellerContext(
            isSeller = true,
            partnerId = data.optLong("partner_id").takeIf { it > 0 },
            displayName = data.optString("display_name").ifBlank { null },
            shop = shop,
            shops = listOf(shop)
        )
    }

    private fun emptyContext() = SellerContext(false, null, null, null, emptyList())

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
