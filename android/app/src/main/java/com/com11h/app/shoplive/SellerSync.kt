package com.com11h.app.shoplive

import android.content.Context
import com.com11h.app.AccountSync
import org.json.JSONArray
import org.json.JSONObject

/**
 * Identity/authorization bridge for the existing COM11H partner account.
 *
 * IMPORTANT: this does NOT create or store a second seller account. The seller
 * identity is always resolved server-side from the same Bearer token used by
 * the existing COM11H customer/web account system.
 */
class SellerSync(context: Context) {
    private val account = AccountSync(context.applicationContext)

    data class SellerContext(
        val isSeller: Boolean,
        val partnerId: Long?,
        val displayName: String?,
        val shops: List<SellerShop>
    )

    data class SellerShop(
        val id: Long,
        val name: String,
        val logoUrl: String?,
        val status: String
    )

    fun context(): SellerContext {
        if (!account.isLoggedIn()) return SellerContext(false, null, null, emptyList())

        val root = account.request("seller_context")
        val data = root.optJSONObject("data") ?: root
        val isSeller = data.optBoolean("is_seller", false)
        if (!root.optBoolean("ok", false) || !isSeller) {
            return SellerContext(false, null, null, emptyList())
        }

        val shopsArray = data.optJSONArray("shops") ?: JSONArray()
        val shops = (0 until shopsArray.length()).mapNotNull { i ->
            shopsArray.optJSONObject(i)?.let { shop ->
                val id = shop.optLong("id")
                if (id <= 0) null else SellerShop(
                    id = id,
                    name = shop.optString("name", "Cửa hàng COM11H"),
                    logoUrl = shop.optString("logo_url").ifBlank { null },
                    status = shop.optString("status", "active")
                )
            }
        }

        return SellerContext(
            isSeller = true,
            partnerId = data.optLong("partner_id").takeIf { it > 0 },
            displayName = data.optString("display_name").ifBlank { null },
            shops = shops
        )
    }
}
