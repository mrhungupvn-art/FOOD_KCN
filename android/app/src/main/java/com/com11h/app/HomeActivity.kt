package com.com11h.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** COM11H v2: Home shell backed by the live COM11H menu/API. */
class HomeActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(3)
    private lateinit var content: LinearLayout
    private val green = Color.rgb(22, 128, 60)
    private val dark = Color.rgb(35, 35, 35)
    private val soft = Color.rgb(247, 249, 247)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Int) = String.format("%,d", v).replace(',', '.') + "đ"
    private fun bg(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun label(value: String, size: Float, color: Int = dark, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun button(value: String, click: () -> Unit) = TextView(this).apply {
        text = value; textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
        background = bg(green, 14); setPadding(dp(12), dp(11), dp(12), dp(11)); setOnClickListener { click() }
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showHome() }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    private fun shell(title: String, selected: Int): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(soft) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(8)); setBackgroundColor(Color.WHITE)
        }
        header.addView(TextView(this).apply { text = "🍚"; textSize = 28f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(50), dp(50)))
        header.addView(label(title, 21f, green, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        header.addView(TextView(this).apply { text = "👤"; textSize = 23f; gravity = Gravity.CENTER; setOnClickListener { route("profile") } }, LinearLayout.LayoutParams(dp(46), dp(46)))
        outer.addView(header)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(18)) }
        scroll.addView(content); outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = dp(8).toFloat() }
        listOf("🏠\nTrang chủ", "🍚\nĐặt món", "🎬\nGiải trí", "🛒\nGiỏ hàng", "👤\nTài khoản").forEachIndexed { i, name ->
            nav.addView(TextView(this).apply {
                text = name; textSize = 10.5f; gravity = Gravity.CENTER; setTextColor(if (i == selected) green else Color.DKGRAY)
                setTypeface(null, if (i == selected) Typeface.BOLD else Typeface.NORMAL); setPadding(0, dp(6), 0, dp(6))
                setOnClickListener { when (i) { 0 -> showHome(); 1 -> route("menu"); 2 -> showEntertainment(); 3 -> route("cart"); 4 -> route("profile") } }
            }, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        outer.addView(nav)
        return outer
    }

    private fun showHome() {
        setContentView(shell("COM11H", 0))
        content.addView(label("Cơm ngon • Nước uống • Ăn vặt", 16f, Color.DKGRAY).apply { gravity = Gravity.CENTER; setPadding(0, dp(2), 0, dp(10)) })
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(18), dp(20), dp(18), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(green, Color.rgb(49, 157, 79))).apply { cornerRadius = dp(22).toFloat() }
        }
        banner.addView(label("🔥 COM11H HÔM NAY", 20f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        banner.addView(label("Đặt cơm nhanh – giao tận nơi", 15f, Color.WHITE).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, dp(12)) })
        banner.addView(TextView(this).apply {
            text = "XEM THỰC ĐƠN"; textSize = 14f; gravity = Gravity.CENTER; setTextColor(green); setTypeface(null, Typeface.BOLD); background = bg(Color.WHITE, 14)
            setPadding(dp(18), dp(10), dp(18), dp(10)); setOnClickListener { route("menu") }
        }, LinearLayout.LayoutParams(-2, -2))
        content.addView(banner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        content.addView(label("🏪 Danh mục trên COM11H", 20f, dark, true).apply { setPadding(0, 0, 0, dp(9)) })
        val categoryBox = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(2)) }
        content.addView(categoryBox)
        content.addView(label("🔥 Món nổi bật", 20f, dark, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        val loading = label("Đang tải món...", 14f, Color.GRAY); content.addView(loading)
        loadFoods(loading, categoryBox)

        content.addView(label("🎬 Giải trí & tin tức", 20f, dark, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14)); background = bg(Color.WHITE, 18) }
        info.addView(label("Video ngắn • Tin mới • Khuyến mãi", 16f, dark, true))
        info.addView(label("COM11H sẽ tập trung nội dung giải trí tại một khu riêng để người dùng vừa xem vừa đặt món.", 13f, Color.GRAY).apply { setPadding(0, dp(5), 0, dp(10)) })
        info.addView(button("🎬 MỞ KHU GIẢI TRÍ") { showEntertainment() }); content.addView(info)
    }

    private fun categoryName(food: JSONObject): String {
        val direct = listOf("category_name", "categoryName", "category", "category_title", "categoryTitle")
        for (key in direct) {
            val value = food.optString(key, "").trim()
            if (value.isNotBlank() && value.lowercase() != "null") return value
        }
        val nested = food.optJSONObject("category")
        if (nested != null) {
            val value = nested.optString("name", nested.optString("title", "")).trim()
            if (value.isNotBlank()) return value
        }
        return "Món ăn"
    }

    private fun addLiveCategories(foods: JSONArray, box: LinearLayout) {
        val names = linkedSetOf<String>()
        for (i in 0 until foods.length()) names.add(categoryName(foods.getJSONObject(i)))
        if (names.isEmpty()) names.add("Tất cả món")

        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        names.take(8).forEach { name ->
            row.addView(TextView(this).apply {
                text = name; textSize = 13f; gravity = Gravity.CENTER; setTextColor(green); setTypeface(null, Typeface.BOLD)
                background = bg(Color.WHITE, 16); setPadding(dp(13), dp(13), dp(13), dp(13)); setOnClickListener { route("menu") }
            }, LinearLayout.LayoutParams(-2, dp(54)).apply { marginEnd = dp(8) })
        }
        horizontal.addView(row); box.addView(horizontal, LinearLayout.LayoutParams(-1, dp(62)))
    }

    private fun loadFoods(loading: TextView, categoryBox: LinearLayout) {
        executor.execute {
            try {
                val c = (URL("https://com11h.com/api/index.php?action=menu").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000; readTimeout = 15000; requestMethod = "GET"; setRequestProperty("Accept", "application/json")
                }
                val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() }); c.disconnect()
                val foods = json.optJSONObject("data")?.optJSONArray("foods") ?: JSONArray()
                runOnUiThread {
                    content.removeView(loading)
                    addLiveCategories(foods, categoryBox)
                    if (foods.length() == 0) { content.addView(label("Chưa có món. Xem thực đơn đầy đủ để cập nhật.", 14f, Color.GRAY)); return@runOnUiThread }
                    for (i in 0 until minOf(5, foods.length())) content.addView(foodCard(foods.getJSONObject(i)))
                    content.addView(button("XEM TẤT CẢ MÓN →") { route("menu") }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
                }
            } catch (_: Exception) {
                runOnUiThread { loading.text = "Không tải được món. Chạm để mở thực đơn."; loading.setOnClickListener { route("menu") } }
            }
        }
    }

    private fun foodCard(food: JSONObject): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(9), dp(10), dp(9)); background = bg(Color.WHITE, 16)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }; setOnClickListener { route("menu") }
        }
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Color.LTGRAY) }
        card.addView(image, LinearLayout.LayoutParams(dp(78), dp(78)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
        info.addView(label(food.optString("name", "Món ăn"), 16f, dark, true)); info.addView(label(money(food.optInt("price")), 15f, green, true).apply { setPadding(0, dp(4), 0, dp(2)) })
        val stock = food.optInt("stock", -1)
        val stockText = if (stock >= 0) "Còn $stock suất" else "Đang bán"
        info.addView(label("$stockText • ${categoryName(food)}", 12f, Color.GRAY).apply { maxLines = 2 }); card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        val imageUrl = food.optString("image"); if (imageUrl.isNotBlank()) loadImage(imageUrl, image); return card
    }

    private fun showEntertainment() {
        setContentView(shell("Giải trí", 2))
        content.addView(label("🎬 Video ngắn & nội dung COM11H", 22f, dark, true))
        content.addView(label("Khu này được chuẩn bị cho kiểu xem dọc: vuốt lên để xem nội dung tiếp theo, đồng thời có thể gắn món ăn vào từng video.", 14f, Color.GRAY).apply { setPadding(0, dp(6), 0, dp(16)) })
        listOf("🎥 VIDEO MÓN NGON" to "Video món ăn, hậu trường bếp và clip ngắn.", "📰 TIN TỨC" to "Món mới, thông báo và hoạt động COM11H.", "🎁 KHUYẾN MÃI" to "Voucher, combo và chương trình ưu đãi.").forEach { (title, desc) ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(15), dp(15), dp(15)); background = bg(Color.WHITE, 18); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) } }
            card.addView(label(title, 18f, green, true)); card.addView(label(desc, 14f, Color.DKGRAY).apply { setPadding(0, dp(5), 0, dp(10)) })
            card.addView(button("MỞ NỘI DUNG") { Toast.makeText(this@HomeActivity, "Sẽ kết nối CMS/API nội dung ở bước tiếp theo.", Toast.LENGTH_SHORT).show() }); content.addView(card)
        }
        content.addView(button("🍚 ĐẶT MÓN NGAY") { route("menu") })
    }

    private fun route(screen: String) { startActivity(Intent(this, LegacyRouterActivity::class.java).putExtra("screen", screen)) }

    private fun loadImage(url: String, view: ImageView) {
        executor.execute {
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 12000
                val b = c.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }; c.disconnect()
                if (b != null) runOnUiThread { view.setImageBitmap(b) }
            } catch (_: Exception) { }
        }
    }
}
