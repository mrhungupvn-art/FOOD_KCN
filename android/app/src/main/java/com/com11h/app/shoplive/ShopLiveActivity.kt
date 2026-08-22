package com.com11h.app.shoplive

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.com11h.app.AccountActivity
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

/** COM11 SHOP LIVE V1 home shell. Existing COM11 activities remain untouched on this branch. */
class ShopLiveActivity : Activity() {
    private lateinit var content: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var api: ShopLiveApi
    private val products = mutableListOf<Product>()
    private val cart = linkedMapOf<Long, CartItem>()
    private val vnd = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = ShopLiveApi(this)
        buildShell()
        showHome()
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val header = TextView(this).apply {
            text = "COM11 SHOP LIVE"; textSize = 21f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), 0, dp(22), 0); setBackgroundColor(Color.rgb(235, 78, 35))
        }
        root.addView(header, LinearLayout.LayoutParams(-1, dp(58)))
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(90)) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = 12f }
        listOf("🏠\nHome", "🛍️\nShop", "🔴\nLIVE", "🛒\nGiỏ", "👤\nTôi").forEachIndexed { i, label ->
            val b = Button(this).apply { text = label; textSize = 11f; setAllCaps(false); setPadding(0, 4, 0, 4) }
            b.setOnClickListener { when (i) { 0 -> showHome(); 1 -> showShop(); 2 -> showLive(); 3 -> showCart(); else -> showAccount() } }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        root.addView(nav); setContentView(root)
    }

    private fun showHome() {
        clear("Xin chào 👋"); addHero("COM11 SHOP LIVE", "Mua sắm • Xem LIVE • Đặt hàng ngay")
        section("🔴 LIVE ĐANG DIỄN RA"); addButton("Xem tất cả LIVE", Color.rgb(235,78,35)) { showLive() }
        addSpace(8); section("🔥 SẢN PHẨM NỔI BẬT"); loadProducts()
    }

    private fun showShop() {
        clear("🛍️ Cửa hàng")
        val search = EditText(this).apply { hint = "Tìm sản phẩm..." }; content.addView(search, lp())
        addButton("🔎 Tìm kiếm", Color.rgb(235,78,35)) { loadProducts(search.text.toString()) }
        section("SẢN PHẨM"); loadProducts()
    }

    private fun showLive() {
        clear("🔴 LIVE"); addHero("LIVE SHOPPING", "Xem người bán trực tiếp và mua ngay trong phòng LIVE")
        addButton("Tạo LIVE (Seller)", Color.rgb(235,78,35)) { startSellerLive() }
        section("LIVE ĐANG DIỄN RA"); loadLiveRooms()
    }

    private fun showCart() {
        clear("🛒 Giỏ hàng")
        if (cart.isEmpty()) { addText("Giỏ hàng đang trống. Hãy chọn sản phẩm từ Shop hoặc LIVE.", 17f); return }
        cart.values.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8) }
            row.addView(TextView(this).apply { text = "${item.product.name}\n${money(item.product.priceVnd)} × ${item.quantity}"; textSize = 16f }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply { text = "Xóa"; setOnClickListener { cart.remove(item.product.id); showCart() } }); content.addView(row, lp())
        }
        addText("Tổng: ${money(cart.values.sumOf { it.subtotalVnd })}", 19f)
        addButton("Đặt hàng", Color.rgb(235,78,35)) { placeOrder() }
    }

    private fun showAccount() {
        clear("👤 Tài khoản")
        if (api.isLoggedIn()) addButton("Đăng xuất", Color.DKGRAY) { api.logout(); showAccount() }
        else {
            addText("Chưa đăng nhập. Shop Live dùng chung tài khoản COM11H hiện tại.", 17f)
            addButton("Mở đăng nhập COM11H", Color.rgb(235,78,35)) { startActivity(android.content.Intent(this, AccountActivity::class.java)) }
        }
        section("NGƯỜI BÁN"); addText("Seller được cấp quyền từ Admin. Sau khi được duyệt, bạn có thể tạo Shop, sản phẩm và LIVE.", 15f)
    }

    private fun loadProducts(q: String? = null) {
        addText("Đang tải sản phẩm...", 14f)
        executor.execute {
            val response = runCatching { api.products(q = q) }.getOrElse { JSONObject().put("ok", false) }
            val parsed = parseProducts(response)
            runOnUiThread {
                if (content.childCount > 0) content.removeViewAt(content.childCount - 1)
                products.clear(); products.addAll(parsed)
                if (parsed.isEmpty()) addText("Chưa có dữ liệu sản phẩm từ API.", 15f) else parsed.forEach { addProductCard(it) }
            }
        }
    }

    private fun loadLiveRooms() {
        addText("Đang tải LIVE...", 14f)
        executor.execute {
            val response = runCatching { api.liveRooms() }.getOrElse { JSONObject().put("ok", false) }
            val rooms = parseLiveRooms(response)
            runOnUiThread {
                if (content.childCount > 0) content.removeViewAt(content.childCount - 1)
                if (rooms.isEmpty()) addText("Chưa có LIVE. Live Server/backend sẽ cung cấp phòng khi được bật.", 15f)
                else rooms.forEach { room -> addCard("🔴 ${room.shopName}\n${room.title}\n${room.viewerCount} người đang xem") { openLive(room) } }
            }
        }
    }

    private fun addProductCard(p: Product) = addCard("🛍️ ${p.name}\n${money(p.priceVnd)}\nKho: ${p.stock}") {
        val old = cart[p.id]?.quantity ?: 0; cart[p.id] = CartItem(p, old + 1)
        Toast.makeText(this, "Đã thêm vào giỏ", Toast.LENGTH_SHORT).show()
    }

    private fun openLive(room: LiveRoom) {
        clear("🔴 ${room.shopName}"); addText(room.title, 21f); addText("${room.viewerCount} người đang xem", 13f)
        if (!room.streamUrl.isNullOrBlank()) {
            val video = VideoView(this).apply { setBackgroundColor(Color.BLACK); setVideoPath(room.streamUrl); setOnPreparedListener { it.start() } }
            content.addView(video, LinearLayout.LayoutParams(-1, dp(210)))
        } else addText("Stream URL chưa được cấp. Live Server sẽ trả HLS/WebRTC URL qua API.", 15f)
        addText("Chat LIVE", 18f)
        val chat = EditText(this).apply { hint = "Viết bình luận..." }; content.addView(chat, lp())
        addButton("Gửi", Color.rgb(235,78,35)) {
            if (room.id > 0 && chat.text.isNotBlank()) { val msg = chat.text.toString(); chat.text.clear(); executor.execute { runCatching { api.sendLiveMessage(room.id, msg) } }; Toast.makeText(this, "Đã gửi", Toast.LENGTH_SHORT).show() }
        }
        addButton("🛒 Xem sản phẩm LIVE", Color.rgb(45,45,45)) { showShop() }
    }

    private fun startSellerLive() {
        if (!api.isLoggedIn()) { showAccount(); return }
        val input = EditText(this).apply { hint = "Tiêu đề LIVE" }; content.addView(input, lp())
        addButton("Bắt đầu LIVE", Color.rgb(235,78,35)) {
            val title = input.text.toString().trim().ifBlank { "LIVE bán hàng COM11" }
            executor.execute {
                val r = runCatching { api.startLive(0, title) }.getOrElse { JSONObject().put("ok", false).put("message", it.message ?: "Lỗi") }
                runOnUiThread { Toast.makeText(this, r.optString("message", "Đã gửi yêu cầu"), Toast.LENGTH_LONG).show(); showLive() }
            }
        }
    }

    private fun placeOrder() { if (!api.isLoggedIn()) showAccount() else Toast.makeText(this, "Đặt hàng sẽ được nối vào order API ở backend V1.", Toast.LENGTH_LONG).show() }

    private fun parseProducts(root: JSONObject): List<Product> {
        val arr = ShopLiveApi.jsonArray(root, "products")
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { o -> Product(o.optLong("id"), o.optLong("shop_id"), o.optString("name"), o.optLong("price_vnd", o.optLong("price")), o.optString("image_url").ifBlank { null }, o.optInt("stock")) } }
    }
    private fun parseLiveRooms(root: JSONObject): List<LiveRoom> {
        val arr = ShopLiveApi.jsonArray(root, "live_rooms")
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { o -> LiveRoom(o.optLong("id"), o.optLong("shop_id"), o.optString("shop_name"), o.optString("title"), o.optString("cover_url").ifBlank { null }, o.optString("stream_url").ifBlank { null }, o.optString("status", "live"), o.optInt("viewer_count"), o.optLong("pinned_product_id").takeIf { it > 0 }) } }
    }
    private fun clear(title: String) { content.removeAllViews(); addText(title, 24f) }
    private fun section(text: String) { addText(text, 18f); addSpace(5) }
    private fun addHero(title: String, sub: String) { content.addView(TextView(this).apply { text = "$title\n$sub"; textSize = 20f; setTextColor(Color.WHITE); setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(Color.rgb(235,78,35)) }, lp()) }
    private fun addText(text: String, size: Float) { content.addView(TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.rgb(35,35,35)); setPadding(4, 9, 4, 9) }, lp()) }
    private fun addCard(text: String, click: () -> Unit) { content.addView(Button(this).apply { this.text = text; textSize = 15f; setAllCaps(false); gravity = Gravity.START or Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(10)); setOnClickListener { click() } }, lp()) }
    private fun addButton(text: String, color: Int, click: () -> Unit) { content.addView(Button(this).apply { this.text = text; setTextColor(Color.WHITE); setBackgroundColor(color); setAllCaps(false); setOnClickListener { click() } }, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, 6, 0, 6) }) }
    private fun addSpace(h: Int) { content.addView(Space(this), LinearLayout.LayoutParams(1, dp(h))) }
    private fun lp() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 5, 0, 5) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Long) = "${vnd.format(v)} đ"
}
