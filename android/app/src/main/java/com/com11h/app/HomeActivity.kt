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

/**
 * COM11H standalone home.
 * No menu/order/cart network calls are made here.
 * The only remote integration in the app is AccountSync (customer account).
 */
class HomeActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val primary = Color.rgb(245, 81, 30)
    private val primaryDark = Color.rgb(208, 67, 21)
    private val accent = Color.rgb(255, 112, 64)
    private val bgColor = Color.rgb(255, 248, 245)
    private val text = Color.rgb(38, 38, 38)
    private val secondary = Color.rgb(107, 107, 107)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showSplash() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }

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
        handler.postDelayed({ showHome() }, 1200)
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
            nav.addView(TextView(this).apply { text = name; textSize = 10.5f; gravity = Gravity.CENTER; setTextColor(if (i == 0) primary else secondary); setTypeface(null, if (i == 0) Typeface.BOLD else Typeface.NORMAL); setPadding(0, dp(5), 0, dp(5)); setOnClickListener { when (i) { 0 -> showHome(); 1 -> open("menu"); 2 -> open("cart"); 3 -> open("orders"); 4 -> open("profile") } } }, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
        outer.addView(nav)
        return outer
    }

    private fun showHome() {
        val shell = shell()
        setContentView(shell)
        val scroll = shell.getChildAt(1) as ScrollView
        val content = scroll.getChildAt(0) as LinearLayout
        content.addView(label("Giao đến", 12f, secondary))
        content.addView(label("📍 Địa chỉ giao hàng của bạn", 14f, text, true).apply { setPadding(0, 0, 0, dp(10)) })
        content.addView(EditText(this).apply { hint = "Tìm món ăn..."; textSize = 14f; isSingleLine = true; setPadding(dp(14), 0, dp(14), 0); background = bg(Color.WHITE, 14) }, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(10) })

        val banner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(17), dp(16), dp(12), dp(16)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(primaryDark, primary, accent)).apply { cornerRadius = dp(20).toFloat() } }
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(label("Cơm ngon\nmỗi ngày", 25f, Color.WHITE, true)); copy.addView(label("Ngon – Sạch – Nhanh", 13f, Color.WHITE).apply { setPadding(0, dp(5), 0, 0) })
        banner.addView(copy, LinearLayout.LayoutParams(0, -2, 1f)); banner.addView(ImageView(this).apply { setImageResource(R.drawable.com11h_logo) }, LinearLayout.LayoutParams(dp(100), dp(88)))
        content.addView(banner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(15) })

        content.addView(label("Đặt món ngay", 20f, text, true).apply { setPadding(0, 0, 0, dp(9)) })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("🍱\nĐặt món", "🍚\nMón ăn", "📦\nĐơn hàng", "🎁\nƯu đãi").forEach { item ->
            shortcuts.addView(TextView(this).apply { text = item; textSize = 13f; gravity = Gravity.CENTER; setTextColor(primary); background = bg(Color.WHITE, 16); setPadding(dp(6), dp(11), dp(6), dp(11)); setOnClickListener { if (item.contains("Đơn")) open("orders") else open("menu") } }, LinearLayout.LayoutParams(0, dp(72), 1f).apply { marginEnd = dp(7) })
        }
        content.addView(shortcuts, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        content.addView(label("Món ăn phổ biến", 20f, text, true).apply { setPadding(0, 0, 0, dp(8)) })
        listOf("Cơm sườn nướng", "Cơm gà xối mỡ", "Cơm bò lúc lắc", "Cơm cá kho tộ").forEachIndexed { i, name ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); background = bg(Color.WHITE, 16); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
            card.addView(TextView(this).apply { text = listOf("🍖", "🍗", "🥩", "🐟")[i]; textSize = 34f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(58), dp(58)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
            info.addView(label(name, 16f, text, true)); info.addView(label(listOf("45.000đ", "42.000đ", "48.000đ", "40.000đ")[i], 15f, primary, true)); info.addView(label("⭐ 4.${7 + (i % 3)}", 12f, secondary)); card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            card.setOnClickListener { open("menu") }
            content.addView(card)
        }
    }

    private fun open(screen: String) { startActivity(Intent(this, MainActivity::class.java).putExtra("screen", screen)) }
}
