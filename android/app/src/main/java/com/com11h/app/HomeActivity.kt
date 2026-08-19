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
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** COM11H redesigned customer home: orange-red brand, welcome splash and food-commerce home. */
class HomeActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(3)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var content: LinearLayout
    private val primary = Color.rgb(245, 81, 30)
    private val primaryDark = Color.rgb(208, 67, 21)
    private val accent = Color.rgb(255, 112, 64)
    private val background = Color.rgb(255, 248, 245)
    private val text = Color.rgb(38, 38, 38)
    private val secondary = Color.rgb(107, 107, 107)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Int) = String.format("%,d", v).replace(',', '.') + "đ"
    private fun bg(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showSplash() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); executor.shutdownNow(); super.onDestroy() }

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
        handler.postDelayed({ showHome() }, 1500)
    }

    private fun label(value: String, size: Float, color: Int = text, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD) }
    private fun actionButton(value: String, click: () -> Unit) = TextView(this).apply { text = value; textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); background = bg(primary, 14); setPadding(dp(14), dp(12), dp(14), dp(12)); setOnClickListener { click() } }

    private fun shell(title: String, selected: Int): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(background) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(9), dp(14), dp(8)); setBackgroundColor(Color.WHITE) }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.com11h_logo); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(label(title, 20f, primary, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        header.addView(TextView(this).apply { text = "👤"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(primary); setPadding(dp(8), dp(6), dp(8), dp(6)); background = bg(Color.rgb(255, 240, 234), 22); setOnClickListener { open("profile") } }, LinearLayout.LayoutParams(dp(44), dp(44)))
        outer.addView(header)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(18)) }
        scroll.addView(content); outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = dp(8).toFloat() }
        listOf("⌂\nTrang chủ", "▦\nThực đơn", "🛒\nGiỏ hàng", "▤\nĐơn hàng", "♙\nTài khoản").forEachIndexed { i, name ->
            nav.addView(TextView(this).apply { text = name; textSize = 10.5f; gravity = Gravity.CENTER; setTextColor(if (i == selected) primary else secondary); setTypeface(null, if (i == selected) Typeface.BOLD else Typeface.NORMAL); setPadding(0, dp(5), 0, dp(5)); setOnClickListener { when (i) { 0 -> showHome(); 1 -> open("menu"); 2 -> open("cart"); 3 -> open("orders"); 4 -> open("profile") } } }, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        outer.addView(nav)
        return outer
    }

    private fun showHome() {
        setContentView(shell("Cơm 11h", 0))
        content.addView(label("Giao đến", 12f, secondary).apply { setPadding(0, 0, 0, dp(2)) })
        content.addView(label("📍 227 Nguyễn Văn Cừ, Q.5, TP.HCM  ▾", 14f, text, true).apply { setPadding(0, 0, 0, dp(10)) })
        val search = EditText(this).apply { hint = "Tìm món ăn, quán..."; textSize = 14f; singleLine = true; setPadding(dp(14), 0, dp(14), 0); background = bg(Color.WHITE, 14) }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(10) })
        val banner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(17), dp(16), dp(12), dp(16)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(primaryDark, primary, accent)).apply { cornerRadius = dp(20).toFloat() } }
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(label("Cơm ngon\nmỗi ngày", 25f, Color.WHITE, true)); copy.addView(label("Ngon – Sạch – Nhanh", 13f, Color.WHITE).apply { setPadding(0, dp(5), 0, 0) })
        banner.addView(copy, LinearLayout.LayoutParams(0, -2, 1f)); banner.addView(ImageView(this).apply { setImageResource(R.drawable.com11h_logo) }, LinearLayout.LayoutParams(dp(100), dp(88)))
        content.addView(banner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(15) })
        content.addView(label("Đặt món ngay", 20f, text, true).apply { setPadding(0, 0, 0, dp(9)) })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("🍱\nĐặt món", "🍚\nMón ăn", "📦\nĐơn hàng", "🎁\nƯu đãi").forEach { item -> shortcuts.addView(TextView(this).apply { text = item; textSize = 13f; gravity = Gravity.CENTER; setTextColor(primary); background = bg(Color.WHITE, 16); setPadding(dp(6), dp(11), dp(6), dp(11)); setOnClickListener { if (item.contains("Đơn")) open("orders") else open("menu") } }, LinearLayout.LayoutParams(0, dp(72), 1f).apply { marginEnd = dp(7) }) }
        content.addView(shortcuts, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        content.addView(label("Món ăn phổ biến", 20f, text, true).apply { setPadding(0, 0, 0, dp(8)) })
        val loading = label("Đang tải món...", 14f, secondary); content.addView(loading); loadFoods(loading)
    }

    private fun loadFoods(loading: TextView) {
        executor.execute {
            try {
                val c = (URL("https://com11h.com/api/index.php?action=menu").openConnection() as HttpURLConnection).apply { connectTimeout = 10000; readTimeout = 15000; requestMethod = "GET"; setRequestProperty("Accept", "application/json") }
                val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() }); c.disconnect(); val foods = json.optJSONObject("data")?.optJSONArray("foods") ?: JSONArray()
                runOnUiThread { content.removeView(loading); for (i in 0 until minOf(4, foods.length())) content.addView(foodCard(foods.getJSONObject(i))); content.addView(actionButton("XEM TẤT CẢ THỰC ĐƠN  →") { open("menu") }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) }) }
            } catch (_: Exception) { runOnUiThread { loading.text = "Không tải được món. Chạm để mở thực đơn."; loading.setOnClickListener { open("menu") } } }
        }
    }

    private fun foodCard(food: JSONObject): LinearLayout {
        val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(9), dp(9), dp(9), dp(9)); background = bg(Color.WHITE, 16); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }; setOnClickListener { open("menu") } }
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Color.rgb(245,245,245)) }; card.addView(image, LinearLayout.LayoutParams(dp(90), dp(90)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(5), 0) }
        info.addView(label(food.optString("name", "Món ăn"), 16f, text, true)); info.addView(label(money(food.optInt("price")), 15f, primary, true).apply { setPadding(0, dp(5), 0, dp(3)) }); info.addView(label("⭐ ${food.optDouble("rating", 4.8)}", 12f, secondary)); card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(TextView(this).apply { text = "+"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = bg(primary, 22); setOnClickListener { open("menu") } }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val url = food.optString("image"); if (url.isNotBlank()) loadImage(url, image); return card
    }

    private fun loadImage(url: String, image: ImageView) { executor.execute { try { val bitmap = android.graphics.BitmapFactory.decodeStream(URL(url).openStream()); runOnUiThread { image.setImageBitmap(bitmap) } } catch (_: Exception) {} } }
    private fun open(screen: String) { startActivity(Intent(this, MainActivity::class.java).putExtra("screen", screen)) }
}
