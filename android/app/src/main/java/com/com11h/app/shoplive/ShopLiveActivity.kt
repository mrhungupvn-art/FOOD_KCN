package com.com11h.app.shoplive

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

/** COM11 SHOP LIVE V1 home shell. Existing COM11 activities remain untouched on this branch. */
class ShopLiveActivity : AppCompatActivity() {
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

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val header = TextView(this).apply {
            text = "COM11 SHOP LIVE"
            textSize = 21f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL
            setPadding(22, 0, 22, 0); setBackgroundColor(Color.rgb(235, 78, 35))
        }
        root.addView(header, LinearLayout.LayoutParams(-1, dp(58)))
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(90)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = 12f }
        listOf("🏠\nHome", "🛍️\nShop", "🔴\nLIVE", "🛒\nGiỏ", "👤\nTôi").forEachIndexed { i, label ->
            val b = Button(this).apply { text = label; textSize = 11f; setAllCaps(false); setPadding(0, 4, 0, 4) }
            b.setOnClickListener { when (i) { 0 -> showHome(); 1 -> showShop(); 2 -> showLive(); 3 -> showCart(); else -> showAccount() } }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun showHome() {
        clear("Xin chào 👋")
        addHero("COM11 SHOP LIVE", "Mua sắm • Xem LIVE • Đặt hàng ngay")
        section("🔴 LIVE ĐANG DIỄN RA")
        addButton("Xem tất cả LIVE", Color.rgb(235,78,35)) { showLive() }
        addSpace(8)
        section("🔥 SẢN PHẨM NỔI BẬT")
        loadProducts()
    }

    private fun showShop() {
        clear("🛍️ Cửa hàng")
        val search = EditText(this).apply { hint = "Tìm sản phẩm..." }
        content.addView(search, lp())
        addButton("🔎 Tìm kiếm", Color.rgb(235,78,35)) { loadProducts(search.text.toString()) }
        section("SẢN PHẨM")
        loadProducts()
    }

    private fun showLive() {
        clear("🔴 LIVE")
        addHero("LIVE SHOPPING", "Xem người bán trực tiếp và mua ngay trong phòng LIVE")
        addButton("Tạo LIVE (Seller)", Color.rgb(235,78,35)) { startSellerLive() }
        section("LIVE ĐANG DIỄN RA")
        loadLiveRooms()
    }

    private fun showCart() {
        clear("🛒 Giỏ hàng")
        if (cart.isEmpty()) {
            addText("Giỏ hàng đang trống. Hãy chọn sản phẩm từ Shop hoặc LIVE.", 17f)
            return
        }
        cart.values.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8) }
            val t = TextView(this).apply { text = "${item.product.name}\n${money(item.product.priceVnd)} × ${item.quantity}"; textSize = 16f }
            row.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
            val del = Button(this).apply { text = "Xóa"; setOnClickListener { cart.remove(item.product.id); showCart() } }
            row.addView(del)
            content.addView(row, lp())
        }
        val total = cart.values.sumOf { it.subtotalVnd }
        addText("Tổng: ${money(total)}", 19f)
        addButton("Đặt hàng", Color.rgb(235,78,35)) { placeOrder() }
    }

    private fun showAccount() {
        clear("👤 Tài khoản")
        if (api.isLoggedIn()) {
            addText("Bạn đang đăng nhập.", 17f)
            addButton("Đăng xuất", Color.DKGRAY) { api.logout(); showAccount() }
        } else {
            addText("Chưa đăng nhập. Hệ thống Shop Live dùng chung tài khoản COM11H hiện tại.", 17f)
            addButton("Mở đăng nhập COM11H", Color.rgb(235,78,35)) {
                startActivity(android.content.Intent(this, com.com11h.app.AccountActivity::class.java))
            }
        }
        section("NGƯỜI BÁN")
        addText("Seller được cấp quyền từ hệ thống quản trị. Sau khi được duyệt, bạn có thể tạo Shop, sản phẩm và LIVE.", 15f)
    }

    private fun loadProducts(q: String? = null) {
        addText("Đang tải sản phẩm...", 14f)
        executor.execute {
            val response = runCatching { api.products(q = q) }.getOrElse { JSONObject().put("ok", false).put("message", it.message ?: "Lỗi kết nối") }
            val parsed = parseProducts(response)
            runOnUiThread {
                content.removeViews(content.childCount - 1, 1)
                products.clear(); products.addAll(parsed)
                if (parsed.isEmpty()) addText("Chưa có dữ liệu sản phẩm từ API. Bạn có thể tiếp tục phát triển bằng database demo.", 15f)
                parsed.forEach { addProductCard(it) }
            }
        }
    }

    private fun loadLiveRooms() {
        addText("Đang tải LIVE...", 14f)
        executor.execute {
            val response = runCatching { api.liveRooms() }.getOrElse { JSONObject().put("ok", false) }
            val rooms = parseLiveRooms(response)
            runOnUiThread {
                content.removeViews(content.childCount - 1, 1)
                if (rooms.isEmpty()) addText("Chưa có LIVE. Seller có thể tạo LIVE sau khi backend được bật.", 15f)
                rooms.forEach { room ->
                    addCard("🔴 ${room.shopName}\n${room.title}\n${room.viewerCount} người đang xem") { openLive(room) }
                }
            }
        }
    }

    private fun addProductCard(p: Product) {
        addCard("🛍️ ${p.name}\n${money(p.priceVnd)}\nKho: ${p.stock}") {
            val old = cart[p.id]?.quantity ?: 0
            cart[p.id] = CartItem(p, old + 1)
            Toast.makeText(this, "Đã thêm vào giỏ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLive(room: LiveRoom) {
        clear("🔴 ${room.shopName}")
        addText(room.title, 21f)
        addText("${room.viewerCount} người đang xem", 13f)
        val video = VideoView(this).apply { setBackgroundColor(Color.BLACK) }
        if (!room.streamUrl.isNullOrBlank()) {
            video.setVideoPath(room.streamUrl)
            video.setOnPreparedListener { it.isLooping = false; video.start() }
        } else {
            addText("Stream URL chưa được cấp. Khi Live Server hoạt động, API sẽ trả về HLS/WebRTC URL tại đây.", 15f)
        }
        content.addView(video, LinearLayout.LayoutParams(-1, dp(210)))
        addText("Chat LIVE", 18f)
        val chat = EditText(this).apply { hint = "Viết bình luận..." }
        content.addView(chat, lp())
        addButton("Gửi", Color.rgb(235,78,35)) {
            if (room.id > 0 && chat.text.isNotBlank()) {
                executor.execute { runCatching { api.sendLiveMessage(room.id, chat.text.toString()) } }
                chat.text.clear(); Toast.makeText(this, "Đã gửi", Toast.LENGTH_SHORT).show()
            }
        }
        addButton("🛒 Xem sản phẩm LIVE", Color.rgb(45,45,45)) { showShop() }
    }

    private fun startSellerLive() {
        if (!api.isLoggedIn()) { showAccount(); return }
        val input = EditText(this).apply { hint = "Tiêu đề LIVE" }
        content.addView(input, lp())
        addButton("Bắt đầu LIVE", Color.rgb(235,78,35)) {
            val title = input.text.toString().trim().ifBlank { "LIVE bán hàng COM11" }
            executor.execute {
                val r = runCatching { api.startLive(0, title) }.getOrElse { JSONObject().put("ok", false).put("message", it.message ?: "Lỗi") }
                runOnUiThread { Toast.makeText(this, r.optString("message", if (r.optBoolean("ok")) "Đã tạo LIVE" else "Không thể tạo LIVE"), Toast.LENGTH_LONG).show(); showLive() }
            }
        }
    }

    private fun placeOrder() {
        if (!api.isLoggedIn()) { showAccount(); return }
        Toast.makeText(this, "Luồng đặt hàng sẽ gửi dữ liệu qua API COM11H ở bước backend V1.", Toast.LENGTH_LONG).show()
    }

    private fun parseProducts(root: JSONObject): List<Product> {
        val arr = ShopLiveApi.jsonArray(root, "products")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Product(o.optLong("id"), o.optLong("shop_id"), o.optString("name"), o.optLong("price_vnd", o.optLong("price")), o.optString("image_url").ifBlank { null }, o.optInt("stock"))
        }
    }

    private fun parseLiveRooms(root: JSONObject): List<LiveRoom> {
        val arr = ShopLiveApi.jsonArray(root, "live_rooms")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            LiveRoom(o.optLong("id"), o.optLong("shop_id"), o.optString("shop_name"), o.optString("title"), o.optString("cover_url").ifBlank { null }, o.optString("stream_url").ifBlank { null }, o.optString("status", "live"), o.optInt("viewer_count"), o.optLong("pinned_product_id").takeIf { it > 0 })
        }
    }

    private fun clear(title: String) { content.removeAllViews(); addText(title, 24f) }
    private fun section(text: String) { addText(text, 18f); addSpace(5) }
    private fun addHero(title: String, sub: String) { val box = TextView(this).apply { text = "$title\n$sub"; textSize = 20f; setTextColor(Color.WHITE); setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(Color.rgb(235,78,35)) }; content.addView(box, lp()) }
    private fun addText(text: String, size: Float) { content.addView(TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.rgb(35,35,35)); setPadding(4, 9, 4, 9) }, lp()) }
    private fun addCard(text: String, click: () -> Unit) { val b = Button(this).apply { this.text = text; textSize = 15f; setAllCaps(false); gravity = Gravity.START or Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(10)); setOnClickListener { click() } }; content.addView(b, lp()) }
    private fun addButton(text: String, color: Int, click: () -> Unit) { val b = Button(this).apply { this.text = text; setTextColor(Color.WHITE); setBackgroundColor(color); setAllCaps(false); setOnClickListener { click() } }; content.addView(b, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, 6, 0, 6) }) }
    private fun addSpace(h: Int) { content.addView(Space(this), LinearLayout.LayoutParams(1, dp(h))) }
    private fun lp() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 5, 0, 5) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Long) = "${vnd.format(v)} đ"
}
