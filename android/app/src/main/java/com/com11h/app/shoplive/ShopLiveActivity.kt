package com.com11h.app.shoplive

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.com11h.app.AccountActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

/**
 * COM11H LIVE shopping UI.
 *
 * Ownership rule: the authenticated Partner owns exactly one shop. The app never
 * asks a Seller to choose another shop; the backend remains the authority.
 */
class ShopLiveActivity : Activity() {
    private lateinit var content: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var api: ShopLiveApi
    private val cart = linkedMapOf<Long, CartItem>()
    private val vnd = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    private var seller: SellerSync.SellerContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = ShopLiveApi(this)
        buildShell()
        showHome()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply {
            text = "COM11H LIVE"
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), 0, dp(22), 0)
            setBackgroundColor(Color.rgb(235, 78, 35))
        }, LinearLayout.LayoutParams(-1, dp(58)))

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(90))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            elevation = 12f
        }
        listOf("🏠\nHome", "🛍️\nShop", "🔴\nLIVE", "🛒\nGiỏ", "👤\nTôi").forEachIndexed { i, label ->
            nav.addView(Button(this).apply {
                text = label
                textSize = 11f
                setAllCaps(false)
                setPadding(0, 4, 0, 4)
                setOnClickListener {
                    when (i) {
                        0 -> showHome()
                        1 -> showShop()
                        2 -> showLive()
                        3 -> showCart()
                        else -> showAccount()
                    }
                }
            }, LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun showHome() {
        clear("Xin chào 👋")
        addHero("COM11H LIVE", "Xem LIVE • Ghim món • Mua ngay")
        section("🔴 LIVE ĐANG DIỄN RA")
        addButton("Xem tất cả LIVE", true) { showLive() }
    }

    private fun showShop() {
        clear("🛍️ Cửa hàng")
        val search = EditText(this).apply { hint = "Tìm món ăn..." }
        content.addView(search, lp())
        addButton("🔎 Tìm kiếm", true) { loadProducts(null, search.text.toString()) }
        section("SẢN PHẨM CỦA SHOP")
        loadProducts()
    }

    private fun showLive() {
        clear("🔴 LIVE")
        addHero("LIVE SHOPPING", "Xem người bán và đặt món ngay trong LIVE")
        addButton("🔴 Seller: Bắt đầu LIVE", true) { startSellerLive() }
        section("LIVE ĐANG DIỄN RA")
        loadLiveRooms()
    }

    private fun showCart() {
        clear("🛒 Giỏ hàng")
        if (cart.isEmpty()) {
            addText("Giỏ hàng đang trống.", 17f)
            return
        }
        cart.values.toList().forEach { item ->
            addCard("${item.product.name}\n${money(item.product.priceVnd)} × ${item.quantity}") {
                cart.remove(item.product.id)
                showCart()
            }
        }
        addText("Tổng: ${money(cart.values.sumOf { it.subtotalVnd })}", 19f)
        addButton("Đặt hàng", true) { placeOrder() }
    }

    private fun showAccount() {
        clear("👤 Tài khoản")
        if (api.isLoggedIn()) {
            addButton("Đăng xuất", false) { api.logout(); showAccount() }
            section("SELLER")
            addButton("🏪 Mở Seller Center", true) { loadSellerContext() }
        } else {
            addText("Đăng nhập bằng tài khoản COM11H hiện tại.", 17f)
            addButton("Mở đăng nhập COM11H", true) {
                startActivity(Intent(this, AccountActivity::class.java))
            }
        }
    }

    private fun loadProducts(shopId: Long? = null, q: String? = null, onLoaded: ((List<Product>) -> Unit)? = null) {
        val loading = addText("Đang tải...", 14f)
        executor.execute {
            val r = runCatching { api.products(shopId, q) }.getOrElse { JSONObject().put("ok", false) }
            val products = parseProducts(r)
            runOnUiThread {
                content.removeView(loading)
                if (products.isEmpty()) addText("Chưa có sản phẩm.", 15f)
                else products.forEach { addProductCard(it) }
                onLoaded?.invoke(products)
            }
        }
    }

    private fun loadLiveRooms() {
        val loading = addText("Đang tải LIVE...", 14f)
        executor.execute {
            val r = runCatching { api.liveRooms() }.getOrElse { JSONObject() }
            val a = ShopLiveApi.jsonArray(r, "live_rooms")
            val rooms = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(::parseRoom) }
            runOnUiThread {
                content.removeView(loading)
                if (rooms.isEmpty()) addText("Chưa có LIVE đang diễn ra.", 15f)
                else rooms.forEach { openRoomCard(it) }
            }
        }
    }

    private fun openRoomCard(room: LiveRoom) {
        addCard("🔴 ${room.shopName}\n${room.title}\n👁 ${room.viewerCount} đang xem\n📌 ${room.pinnedProductIds.size} món nổi bật") {
            openLive(room)
        }
    }

    private fun addProductCard(product: Product, label: String = "🛍️") {
        val stockText = if (product.stock > 0) "Kho ${product.stock}" else "Hết hàng"
        addCard("$label ${product.name}\n${money(product.priceVnd)} • $stockText") {
            if (!product.isActive || product.stock <= 0) {
                Toast.makeText(this, "Món hiện không còn bán.", Toast.LENGTH_SHORT).show()
                return@addCard
            }
            val n = cart[product.id]?.quantity ?: 0
            cart[product.id] = CartItem(product, n + 1)
            Toast.makeText(this, "Đã thêm ${product.name} vào giỏ", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Customer LIVE screen: pinned products are a promoted layer at the top,
     * while the complete active shop menu remains available below.
     */
    private fun openLive(room: LiveRoom) {
        clear("🔴 ${room.shopName}")
        addText(room.title, 21f)
        addText("👁 ${room.viewerCount} người đang xem", 13f)

        if (!room.streamUrl.isNullOrBlank()) {
            val video = VideoView(this).apply {
                setBackgroundColor(Color.BLACK)
                setVideoPath(room.streamUrl)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@ShopLiveActivity, "Không thể phát LIVE.", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            content.addView(video, LinearLayout.LayoutParams(-1, dp(220)))
        } else {
            addText("LIVE đang kết nối với máy chủ phát hình...", 15f)
        }

        section("📌 SẢN PHẨM NỔI BẬT / ĐANG GHIM")
        val pinnedSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(pinnedSection, lp())
        section("🛍️ TẤT CẢ SẢN PHẨM CỦA SHOP")
        val allSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(allSection, lp())

        addText("Món ghim không bị xóa khỏi menu; chỉ được ưu tiên hiển thị phía trên.", 12f)
        section("💬 CHAT LIVE")
        val chat = EditText(this).apply { hint = "Viết bình luận..." }
        content.addView(chat, lp())
        addButton("Gửi", true) {
            val msg = chat.text.toString().trim()
            if (msg.isNotEmpty()) {
                chat.text.clear()
                executor.execute { runCatching { api.sendLiveMessage(room.id, msg) } }
            }
        }

        executor.execute {
            val result = runCatching { api.products(room.shopId) }.getOrElse { JSONObject() }
            val products = parseProducts(result).filter { it.isActive }
            val pinned = products.filter { room.pinnedProductIds.contains(it.id) }
                .sortedBy { room.pinnedProductIds.indexOf(it.id) }
            val normal = products.filterNot { room.pinnedProductIds.contains(it.id) }
            runOnUiThread {
                if (pinned.isEmpty()) addTextTo(pinnedSection, "Chưa có món ghim.", 14f)
                else pinned.forEachIndexed { index, p -> addButtonTo(pinnedSection, "${index + 1}. ⭐ ${p.name}\n${money(p.priceVnd)}", true) { addToCart(p) } }
                if (normal.isEmpty()) addTextTo(allSection, "Chưa có món đang bán.", 14f)
                else normal.forEach { p -> addButtonTo(allSection, "${p.name}\n${money(p.priceVnd)} • Kho ${p.stock}", true) { addToCart(p) } }
            }
        }
    }

    private fun addToCart(product: Product) {
        if (product.stock <= 0) {
            Toast.makeText(this, "Món đã hết.", Toast.LENGTH_SHORT).show()
            return
        }
        val n = cart[product.id]?.quantity ?: 0
        cart[product.id] = CartItem(product, n + 1)
        Toast.makeText(this, "Đã thêm ${product.name} vào giỏ", Toast.LENGTH_SHORT).show()
    }

    private fun loadSellerContext() {
        executor.execute {
            val r = runCatching { api.sellerContext() }.getOrElse { JSONObject().put("ok", false) }
            val d = r.optJSONObject("data") ?: r
            runOnUiThread {
                if (!r.optBoolean("ok", false) || !d.optBoolean("is_seller", false)) {
                    clear("🏪 SELLER CENTER")
                    addText("Tài khoản này chưa được cấp quyền Seller/Đối tác.", 16f)
                    return@runOnUiThread
                }
                seller = runCatching { SellerSync(this).context() }.getOrNull()
                val shop = seller?.shops?.firstOrNull()
                clear("🏪 SELLER CENTER")
                addText("Xin chào ${seller?.displayName ?: "Seller"}", 21f)
                if (shop == null) {
                    addText("Tài khoản Seller chưa được gắn cửa hàng.", 16f)
                    return@runOnUiThread
                }
                addCard("🏪 ${shop.name}\nTrạng thái: ${shop.status}") { sellerShopMenu(shop) }
            }
        }
    }

    /** Exactly one shop is resolved from the Partner account. */
    private fun sellerShopMenu(shop: SellerSync.SellerShop) {
        clear("🏪 ${shop.name}")
        addText("Seller Center • chỉ quản lý cửa hàng này", 18f)
        addButton("🔴 Tạo LIVE", true) { sellerCreateLive(shop) }
        addButton("🍱 Sản phẩm của tiệm", true) { sellerProducts(shop) }
        addText("Seller không được chọn shop khác. Backend phải kiểm tra quyền sở hữu ở mọi request.", 13f)
    }

    private fun sellerProducts(shop: SellerSync.SellerShop) {
        clear("🍱 ${shop.name}")
        addText("Chỉ sản phẩm thuộc cửa hàng này được sử dụng trong LIVE.", 14f)
        loadProducts(shop.id)
    }

    private fun sellerCreateLive(shop: SellerSync.SellerShop) {
        clear("🔴 Tạo LIVE")
        addText("Cửa hàng: ${shop.name}", 18f)
        val title = EditText(this).apply { hint = "Tiêu đề LIVE" }
        content.addView(title, lp())
        addText("Sau khi LIVE được tạo, Seller có thể ghim tối đa 5 món của chính cửa hàng.", 13f)
        addButton("BẮT ĐẦU LIVE", true) {
            val t = title.text.toString().trim().ifBlank { "LIVE tại ${shop.name}" }
            executor.execute {
                val r = runCatching { api.startLive(shop.id, t) }
                    .getOrElse { JSONObject().put("ok", false).put("message", it.message ?: "Lỗi kết nối") }
                runOnUiThread {
                    Toast.makeText(this, r.optString("message", if (r.optBoolean("ok")) "Đã tạo LIVE" else "Không thể tạo LIVE"), Toast.LENGTH_LONG).show()
                    val data = r.optJSONObject("data")
                    val roomId = data?.optLong("room_id", data.optLong("id", 0)) ?: 0
                    if (r.optBoolean("ok") && roomId > 0) sellerLiveControls(shop, roomId)
                    else showLive()
                }
            }
        }
    }

    /** Seller control: only own shop products can be pinned. */
    private fun sellerLiveControls(shop: SellerSync.SellerShop, roomId: Long) {
        clear("🔴 LIVE • ${shop.name}")
        addText("Phiên LIVE #$roomId", 19f)
        addButton("⏹ Kết thúc LIVE", false) {
            executor.execute {
                val r = runCatching { api.stopLive(roomId) }.getOrElse { JSONObject().put("ok", false) }
                runOnUiThread {
                    Toast.makeText(this, r.optString("message", "Đã gửi yêu cầu kết thúc LIVE"), Toast.LENGTH_SHORT).show()
                    showLive()
                }
            }
        }
        section("📌 GHIM SẢN PHẨM CỦA TIỆM")
        loadProducts(shop.id) { products ->
            products.filter { it.isActive && it.stock > 0 }.forEach { product ->
                addButton("📌 ${product.name} • ${money(product.priceVnd)}", true) {
                    executor.execute {
                        val r = runCatching { api.pinLiveProduct(roomId, product.id) }.getOrElse { JSONObject() }
                        runOnUiThread { Toast.makeText(this, r.optString("message", "Đã gửi yêu cầu ghim"), Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
        addText("Backend giới hạn tối đa 5 sản phẩm ghim và xác minh product.shop_id == seller.shop_id.", 12f)
    }

    private fun startSellerLive() {
        if (!api.isLoggedIn()) {
            showAccount()
            return
        }
        loadSellerContext()
    }

    private fun placeOrder() {
        if (!api.isLoggedIn()) {
            showAccount()
            return
        }
        // Deliberately do not invent a second order endpoint. The production
        // checkout must call the existing COM11H order/business flow.
        Toast.makeText(this, "Thanh toán sẽ dùng hệ thống đơn hàng COM11H hiện tại.", Toast.LENGTH_LONG).show()
    }

    private fun parseProducts(root: JSONObject): List<Product> {
        val a = ShopLiveApi.jsonArray(root, "products")
        return (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let { o ->
                val id = o.optLong("id")
                if (id <= 0) null else Product(
                    id = id,
                    shopId = o.optLong("shop_id"),
                    name = o.optString("name", "Sản phẩm"),
                    priceVnd = o.optLong("price_vnd", o.optLong("price")),
                    imageUrl = o.optString("image_url").ifBlank { null },
                    stock = o.optInt("stock", 0),
                    isActive = o.optBoolean("is_active", true)
                )
            }
        }
    }

    private fun parseRoom(o: JSONObject): LiveRoom {
        val pinned = mutableListOf<Long>()
        val ids = o.optJSONArray("pinned_product_ids") ?: o.optJSONObject("data")?.optJSONArray("pinned_product_ids")
        if (ids != null) for (i in 0 until ids.length()) ids.optLong(i).takeIf { it > 0 }?.let(pinned::add)
        o.optLong("pinned_product_id").takeIf { it > 0 && !pinned.contains(it) }?.let { pinned.add(0, it) }
        return LiveRoom(
            id = o.optLong("id"),
            shopId = o.optLong("shop_id"),
            shopName = o.optString("shop_name", "Cửa hàng COM11H"),
            title = o.optString("title", "LIVE"),
            coverUrl = o.optString("cover_url").ifBlank { null },
            streamUrl = o.optString("stream_url").ifBlank { null },
            status = o.optString("status", "live"),
            viewerCount = o.optInt("viewer_count", 0),
            pinnedProductId = pinned.firstOrNull(),
            pinnedProductIds = pinned.distinct().take(5)
        )
    }

    private fun clear(title: String) { content.removeAllViews(); addText(title, 24f) }
    private fun section(title: String) { addText(title, 18f); addSpace(5) }
    private fun addHero(title: String, subtitle: String) {
        content.addView(TextView(this).apply {
            text = "$title\n$subtitle"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(Color.rgb(235, 78, 35))
        }, lp())
    }
    private fun addText(text: String, size: Float): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.rgb(35, 35, 35))
        setPadding(4, 9, 4, 9)
        content.addView(this, lp())
    }
    private fun addTextTo(parent: LinearLayout, text: String, size: Float) {
        parent.addView(TextView(this).apply { this.text = text; textSize = size; setPadding(4, 8, 4, 8) }, lp())
    }
    private fun addCard(text: String, click: () -> Unit) {
        content.addView(Button(this).apply {
            this.text = text
            textSize = 15f
            setAllCaps(false)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { click() }
        }, lp())
    }
    private fun addButton(text: String, primary: Boolean, click: () -> Unit) {
        content.addView(Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(if (primary) Color.rgb(235, 78, 35) else Color.DKGRAY)
            setAllCaps(false)
            setOnClickListener { click() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, 6, 0, 6) })
    }
    private fun addButtonTo(parent: LinearLayout, text: String, primary: Boolean, click: () -> Unit) {
        parent.addView(Button(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(if (primary) Color.rgb(235, 78, 35) else Color.DKGRAY)
            setAllCaps(false)
            setGravity(Gravity.START or Gravity.CENTER_VERTICAL)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { click() }
        }, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0, 4, 0, 4) })
    }
    private fun addSpace(height: Int) { content.addView(Space(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun lp() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 5, 0, 5) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun money(value: Long) = "${vnd.format(value)} đ"
}
