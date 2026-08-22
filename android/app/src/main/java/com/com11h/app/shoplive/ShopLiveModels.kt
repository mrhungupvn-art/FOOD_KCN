package com.com11h.app.shoplive

/** Data models for COM11H LIVE. A LIVE room belongs to exactly one shop. */
data class Shop(val id: Long, val name: String, val logoUrl: String? = null, val description: String = "", val sellerUserId: Long? = null)
data class Product(val id: Long, val shopId: Long, val name: String, val priceVnd: Long, val imageUrl: String? = null, val stock: Int = 0, val isActive: Boolean = true)
data class CartItem(val product: Product, val quantity: Int) { val subtotalVnd: Long get() = product.priceVnd * quantity }
data class OrderItem(val productId: Long, val productName: String, val unitPriceVnd: Long, val quantity: Int)
data class Order(val id: Long, val status: String, val totalVnd: Long, val items: List<OrderItem> = emptyList())
/** pinnedProductIds is ordered: index 0 is the highest-priority pinned product. */
data class LiveRoom(val id: Long, val shopId: Long, val shopName: String, val title: String, val coverUrl: String? = null, val streamUrl: String? = null, val status: String = "live", val viewerCount: Int = 0, val pinnedProductId: Long? = null, val pinnedProductIds: List<Long> = emptyList())
data class LiveMessage(val id: Long, val userName: String, val message: String, val createdAt: String)
