package com.com11h.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Trang chủ COM11H. "Món ăn phổ biến" lấy trực tiếp từ api?action=menu (cùng
 * dữ liệu với web) qua AccountSync — không còn danh sách món ăn giả lập.
 * Banner ở giữa trang và ô tìm kiếm cũng đồng bộ trực tiếp với server:
 *   - Banner: lấy từ api?action=banners, cùng dữ liệu Admin > Banner trang
 *     chủ đang quản lý cho web (admin/banners.php) — đổi banner trên Admin
 *     là app tự cập nhật theo, không cần sửa code app.
 *   - Ô tìm kiếm: có nút bấm 🔍 (và bấm "Tìm kiếm" trên bàn phím) để mở
 *     màn Thực đơn và lọc sẵn theo từ khoá đã nhập.
 */
class HomeActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var account: AccountSync
    private val primary = Color.rgb(245, 81, 30)
    private val primaryDark = Color.rgb(208, 67, 21)
    private val accent = Color.rgb(255, 112, 64)
    private val bgColor = Color.rgb(255, 248, 245)
    private val text = Color.rgb(38, 38, 38)
    private val secondary = Color.rgb(107, 107, 107)
    // Badge số lượng trên icon 🛒 Giỏ hàng ở thanh điều hướng — cập nhật mỗi khi
    // dựng lại trang chủ và mỗi khi quay lại trang chủ từ màn hình khác (onResume).
    private var cartBadge: TextView? = null
    // Khung chứa "Món ăn phổ biến" — giữ lại tham chiếu để có thể tải & xáo lại
    // danh sách món mỗi khi khách quay lại trang chủ (onResume), không chỉ lúc
    // dựng trang lần đầu.
    private var popularBox: LinearLayout? = null

    companion object { private const val SITE_URL = "https://com11h.com" }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); account = AccountSync(this); showSplash() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); executor.shutdownNow(); super.onDestroy() }
    // Khách có thể đã thêm/bớt món ở màn Thực đơn hoặc Giỏ hàng rồi bấm Back
    // quay lại đây (không tạo lại Activity) — cập nhật badge cho khớp giỏ hàng mới
    // nhất, đồng thời tải & xáo lại "Món ăn phổ biến" để mỗi lần quay về trang chủ
    // khách luôn thấy các món khác nhau, không cố định mãi cùng vài món.
    override fun onResume() { super.onResume(); refreshCartBadge(); loadPopularFoods() }

    /** Cập nhật số lượng (badge đỏ) trên icon 🛒 Giỏ hàng ở thanh điều hướng, đọc từ giỏ hàng cục bộ đã lưu. */
    private fun refreshCartBadge() {
        val n = CartStore.totalQty(this)
        cartBadge?.apply {
            text = if (n > 99) "99+" else n.toString()
            visibility = if (n > 0) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun showSplash() {
        val root = FrameLayout(this)
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(primary, accent, Color.rgb(255, 244, 232)))
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28), dp(30), dp(28), dp(30)) }
        box.addView(ImageView(this).apply { setImageResource(R.drawable.com11h_logo); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(230), dp(230)))
        box.addView(TextView(this).apply { text = "Cơm 11h"; textSize = 48f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD_ITALIC); gravity = Gravity.CENTER })
        box.addView(TextView(this).apply { text = "xin chào quý khách ❤️"; textSize = 27f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, dp(3), 0, dp(20)) })
        box.addView(TextView(this).apply { text = "Ngon mỗi ngày • Nóng hổi • Giao tận nơi"; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        root.addView(box, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        setContentView(root)
        handler.postDelayed({ showHome() }, 3000)
    }

    private fun label(value: String, size: Float, color: Int = text, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD) }

    private fun shell(): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bgColor) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(9), dp(14), dp(8)); setBackgroundColor(Color.WHITE) }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.com11h_logo); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(label("Cơm 11h", 20f, primary, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        header.addView(TextView(this).apply { text = "👤"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(primary); background = bg(Color.rgb(255, 240, 234), 22); setOnClickListener { open("profile") } }, LinearLayout.LayoutParams(dp(44), dp(44)))
        outer.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(20)) }
        scroll.addView(content)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = dp(8).toFloat() }
        listOf("⌂\nTrang chủ", "▦\nThực đơn", "🛒\nGiỏ hàng", "▤\nĐơn hàng", "♙\nTài khoản").forEachIndexed { i, name ->
            val cell = FrameLayout(this)
            cell.addView(TextView(this).apply { text = name; textSize = 10.5f; gravity = Gravity.CENTER; setTextColor(if (i == 0) primary else secondary); setTypeface(null, if (i == 0) Typeface.BOLD else Typeface.NORMAL); setPadding(0, dp(5), 0, dp(5)); setOnClickListener { when (i) { 0 -> showHome(); 1 -> open("menu"); 2 -> open("cart"); 3 -> open("orders"); 4 -> open("profile") } } }, FrameLayout.LayoutParams(-1, -1))
            if (i == 2) {
                cartBadge = TextView(this).apply {
                    textSize = 10f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD)
                    background = bg(Color.rgb(220, 38, 38), 20)
                    visibility = android.view.View.GONE
                }
                cell.addView(cartBadge, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP or Gravity.END).apply { topMargin = dp(2); marginEnd = dp(14) })
            }
            nav.addView(cell, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        outer.addView(nav)
        refreshCartBadge()
        return outer
    }

    /** Mở màn Thực đơn, lọc sẵn theo từ khoá tìm kiếm đã nhập ở trang chủ. */
    private fun runSearch(keyword: String) {
        val q = keyword.trim()
        val i = Intent(this, MainActivity::class.java).putExtra("screen", "menu")
        if (q.isNotEmpty()) i.putExtra("query", q)
        startActivity(i)
    }

    private fun searchBox(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val input = EditText(this).apply {
            hint = "Tìm món ăn..."
            textSize = 15f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(dp(14), 0, dp(14), 0)
            background = bg(Color.WHITE, 14)
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) { runSearch(v.text.toString()); true } else false
            }
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(46), 1f))
        row.addView(TextView(this).apply {
            text = "🔍"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = bg(primary, 14)
            contentDescription = "Tìm kiếm"
            setOnClickListener { runSearch(input.text.toString()) }
        }, LinearLayout.LayoutParams(dp(46), dp(46)).apply { marginStart = dp(8) })
        return row
    }

    /** Banner tĩnh dự phòng khi Admin chưa tạo banner nào hoặc chưa tải được dữ liệu. */
    private fun staticBanner(): LinearLayout {
        val banner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(17), dp(16), dp(12), dp(16)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(primaryDark, primary, accent)).apply { cornerRadius = dp(20).toFloat() } }
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(label("Cơm ngon\nmỗi ngày", 25f, Color.WHITE, true)); copy.addView(label("Ngon – Sạch – Nhanh", 13f, Color.WHITE).apply { setPadding(0, dp(5), 0, 0) })
        banner.addView(copy, LinearLayout.LayoutParams(0, -2, 1f)); banner.addView(ImageView(this).apply { setImageResource(R.drawable.com11h_logo) }, LinearLayout.LayoutParams(dp(100), dp(88)))
        return banner
    }

    /**
     * Bấm vào banner: xem ảnh PHÓNG TO ngay trong app (không nhảy sang link
     * đích/website nữa) — khách chụm/mở 2 ngón tay để phóng to, thu nhỏ, kéo
     * xem chi tiết nội dung trên ảnh. Vẫn âm thầm gọi banner_click.php ở nền
     * (không mở màn hình nào) để Admin tiếp tục nhận đúng số lượt click banner
     * như trước — chỉ khác là app không điều hướng khách ra khỏi màn hình nữa.
     */
    private fun openBanner(id: Int, imageUrl: String, title: String) {
        if (id > 0) executor.execute {
            try { (java.net.URL("$SITE_URL/banner_click.php?id=$id").openConnection() as java.net.HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000; requestMethod = "GET" }.inputStream.close() } catch (_: Exception) { }
        }
        startActivity(Intent(this, BannerViewActivity::class.java).putExtra("image", imageUrl).putExtra("title", title))
    }

    /**
     * Bấm vào ảnh món ăn (ở "Món ăn phổ biến"): xem ảnh PHÓNG TO ngay trong
     * app, dùng lại đúng màn hình zoom của banner (BannerViewActivity) —
     * khách chụm/mở 2 ngón tay để phóng to, thu nhỏ, kéo xem chi tiết ảnh.
     */
    private fun openFoodImage(imageUrl: String, title: String) {
        if (imageUrl.isBlank()) return
        startActivity(Intent(this, BannerViewActivity::class.java).putExtra("image", imageUrl).putExtra("title", title))
    }

    /** Tải banner trang chủ từ api?action=banners (đồng bộ Admin > Banner trang chủ) và hiển thị dạng slider tự chạy. */
    private fun loadBanners(container: FrameLayout) {
        executor.execute {
            val r = try { account.request("banners") } catch (_: Exception) { null }
            runOnUiThread {
                val arr = r?.optJSONObject("data")?.optJSONArray("banners") ?: JSONArray()
                if (r == null || !r.optBoolean("ok") || arr.length() == 0) {
                    container.removeAllViews(); container.addView(staticBanner(), FrameLayout.LayoutParams(-1, dp(120)))
                    return@runOnUiThread
                }

                val flipper = ViewFlipper(this).apply {
                    inAnimation = AnimationUtils.loadAnimation(this@HomeActivity, android.R.anim.fade_in)
                    outAnimation = AnimationUtils.loadAnimation(this@HomeActivity, android.R.anim.fade_out)
                    flipInterval = 4000
                }
                for (i in 0 until arr.length()) {
                    val b = arr.getJSONObject(i)
                    val id = b.optInt("id")
                    val title = b.optString("title")
                    val imageUrl = b.optString("image")
                    val slide = FrameLayout(this)
                    val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                    slide.addView(img, FrameLayout.LayoutParams(-1, -1))
                    if (title.isNotBlank()) {
                        slide.addView(TextView(this).apply {
                            text = title; textSize = 13f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
                            setPadding(dp(14), dp(8), dp(14), dp(8))
                            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.TRANSPARENT, Color.argb(160, 0, 0, 0)))
                        }, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
                    }
                    slide.clipToOutline = true
                    slide.background = bg(Color.rgb(255, 245, 240), 20)
                    slide.setOnClickListener { openBanner(id, imageUrl, title) }
                    flipper.addView(slide)
                    ImageLoader.load(img, b.optString("image"))
                }
                container.removeAllViews()
                container.addView(flipper, FrameLayout.LayoutParams(-1, dp(120)))
                if (arr.length() > 1) flipper.startFlipping()
            }
        }
    }

    /**
     * Tải "Món ăn phổ biến" từ api?action=menu và hiển thị NGẪU NHIÊN 6 món
     * (xáo trộn lại toàn bộ danh sách món đang bán mỗi lần gọi hàm này) — nhờ
     * vậy mỗi lần khách mở lại trang chủ (kể cả bấm Back từ Thực đơn/Giỏ hàng
     * quay về, xem onResume) sẽ thấy các món khác nhau, không cố định mãi
     * cùng vài món như trước (trước đây luôn lấy đúng 4 món đầu danh sách).
     */
    private fun loadPopularFoods() {
        val popularBox = this.popularBox ?: return
        popularBox.removeAllViews()
        val popularLoading = label("⏳ Đang tải món ăn...", 15f, secondary)
        popularBox.addView(popularLoading)
        executor.execute {
            val r = account.request("menu")
            runOnUiThread {
                if (popularBox != this.popularBox) return@runOnUiThread // màn hình đã đổi/hủy trong lúc chờ tải
                popularBox.removeView(popularLoading)
                val arr = r.optJSONObject("data")?.optJSONArray("foods") ?: JSONArray()
                if (arr.length() == 0) { popularBox.addView(label("Chưa có món nào đang bán.", 15f, secondary)); return@runOnUiThread }
                val list = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                list.shuffle()
                val count = minOf(6, list.size)
                for (i in 0 until count) {
                    val f = list[i]
                    val name = f.optString("name")
                    val imageUrl = f.optString("image")
                    val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); background = bg(Color.WHITE, 16); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
                    // Ảnh món ăn to hơn trước và có thể bấm vào để xem phóng to
                    // (chụm/mở 2 ngón tay để zoom), giống hệt cách xem banner.
                    val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = bg(Color.rgb(255, 245, 240), 14); clipToOutline = true }
                    card.addView(img, LinearLayout.LayoutParams(dp(68), dp(68))); ImageLoader.load(img, imageUrl)
                    img.setOnClickListener { openFoodImage(imageUrl, name) }
                    val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
                    info.addView(label(name, 17f, text, true)); info.addView(label(String.format("%,d", f.optInt("price")).replace(',', '.') + "đ", 16f, primary, true)); info.addView(label("còn ${f.optInt("stock")} phần", 13f, secondary)); card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                    info.setOnClickListener { open("menu") }
                    popularBox.addView(card)
                }
            }
        }
    }

    private fun showHome() {
        val shell = shell()
        setContentView(shell)
        val scroll = shell.getChildAt(1) as ScrollView
        val content = scroll.getChildAt(0) as LinearLayout
        content.addView(label("Giao đến", 13f, secondary))
        content.addView(label("📍 Địa chỉ giao hàng của bạn", 15f, text, true).apply { setPadding(0, 0, 0, dp(10)) })
        content.addView(searchBox(), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val bannerContainer = FrameLayout(this)
        bannerContainer.addView(staticBanner(), FrameLayout.LayoutParams(-1, dp(120)))
        content.addView(bannerContainer, LinearLayout.LayoutParams(-1, dp(120)).apply { bottomMargin = dp(15) })
        loadBanners(bannerContainer)

        content.addView(label("Đặt món ngay", 21f, text, true).apply { setPadding(0, 0, 0, dp(9)) })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("🍱\nĐặt món", "🍚\nMón ăn", "📦\nĐơn hàng", "🎁\nƯu đãi").forEach { item ->
            shortcuts.addView(TextView(this).apply { text = item; textSize = 13.5f; gravity = Gravity.CENTER; setTextColor(primary); background = bg(Color.WHITE, 16); setPadding(dp(6), dp(11), dp(6), dp(11)); setOnClickListener { when { item.contains("Đơn") -> open("orders"); item.contains("Ưu đãi") -> open("lucky"); else -> open("menu") } } }, LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginEnd = dp(7) })
        }
        content.addView(shortcuts, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        content.addView(label("Món ăn phổ biến", 21f, text, true).apply { setPadding(0, 0, 0, dp(8)) })
        val popularBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(popularBox)
        this.popularBox = popularBox
        loadPopularFoods()
    }

    private fun open(screen: String) { startActivity(Intent(this, MainActivity::class.java).putExtra("screen", screen)) }
}
