package com.com11h.app.shoplive

/**
 * Pure client-side display rules for COM11H LIVE.
 *
 * Security is enforced by the API; these rules only keep the UI predictable.
 */
object LiveProductRules {
    const val MAX_PINNED = 5

    data class ProductRef(
        val id: Long,
        val shopId: Long,
        val active: Boolean = true,
        val stock: Int = 0
    )

    fun canUseInLive(product: ProductRef, sellerShopId: Long): Boolean =
        product.shopId == sellerShopId && product.active && product.stock > 0

    fun pinnedOrder(ids: List<Long>): List<Long> =
        ids.distinct().take(MAX_PINNED)

    fun promotedFirst(all: List<ProductRef>, pinnedIds: List<Long>, sellerShopId: Long): List<ProductRef> {
        val allowed = all.filter { canUseInLive(it, sellerShopId) }
        val order = pinnedOrder(pinnedIds)
        val pinned = order.mapNotNull { id -> allowed.firstOrNull { it.id == id } }
        val rest = allowed.filterNot { p -> order.contains(p.id) }
        return pinned + rest
    }
}
