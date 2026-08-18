package com.com11h.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Tài khoản COM11H: đọc dữ liệu thật từ cùng API với website.
 * Điểm và mã quay thưởng tuyệt đối không tính ở Android.
 */
class AccountActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(2)
    private lateinit var root: LinearLayout
    private val green = Color.rgb(22, 128, 60)
    private val dark = Color.rgb(35, 35, 35)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Int) = String.format("%,d", v).replace(',', '.') + "đ"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAccount()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun shell(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 248))
            setPadding(dp(16), dp(14), dp(16), dp(16))
        }
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        scroll.addView(root)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return outer
    }

    private fun label(text: String, size: Float = 16f, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(dark)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 15f
        minimumHeight = dp(46)
        setOnClickListener { action() }
    }

    private fun request(action: String): JSONObject {
        val prefs = getSharedPreferences("com11h_secure", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null) ?: throw IllegalStateException("Chưa đăng nhập")
        val url = URL("https://com11h.com/api/index.php?action=${URLEncoder.encode(action, "UTF-8")}")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            JSONObject(stream?.bufferedReader()?.use { it.readText() } ?: "{}")
        } finally { c.disconnect() }
    }

    private fun showAccount() {
        setContentView(shell())
        root.addView(label("👤 Tài khoản COM11H", 25f, true))
        val loading = label("Đang đồng bộ tài khoản, đơn hàng và mã quay thưởng...", 16f)
        root.addView(loading)

        executor.execute {
            try {
                val profile = request("profile")
                val orders = request("orders")
                runOnUiThread {
                    root.removeView(loading)
                    if (!profile.optBoolean("ok")) {
                        Toast.makeText(this, profile.optString("message", "Phiên đăng nhập đã hết hạn"), Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, MainActivity::class.java).putExtra("screen", "profile"))
                        finish()
                        return@runOnUiThread
                    }
                    renderAccount(profile.getJSONObject("data").getJSONObject("customer"), orders)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading.text = "Không đồng bộ được dữ liệu. ${e.message ?: "Vui lòng thử lại."}"
                    root.addView(button("🔄 Thử lại") { showAccount() })
                }
            }
        }
    }

    private fun renderAccount(customer: JSONObject, ordersResponse: JSONObject) {
        root.addView(label("Họ tên: ${customer.optString("name")}", 17f, true))
        root.addView(label("Số điện thoại: ${customer.optString("phone")}", 16f))

        val pointsBox = TextView(this).apply {
            text = "⭐ Điểm tích luỹ\n${customer.optInt("points")} điểm"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(green)
            setPadding(dp(14), dp(18), dp(14), dp(18))
        }
        root.addView(pointsBox)

        root.addView(label("🎁 Mã quay thưởng của tôi", 21f, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        val orders = ordersResponse.optJSONObject("data")?.optJSONArray("orders")
        var rewardCount = 0
        if (orders != null) {
            for (i in 0 until orders.length()) {
                val o = orders.getJSONObject(i)
                val lucky = o.optString("lucky_code", "").trim()
                if (lucky.isBlank()) continue
                rewardCount++
                val code = o.optString("code")
                val status = o.optString("status")
                val created = o.optString("created_at")
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    setBackgroundColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
                }
                card.addView(label("🎟️ $lucky", 21f, true).apply { setTextColor(green) })
                card.addView(label("Đơn: $code", 14f))
                if (created.isNotBlank()) card.addView(label("Ngày: $created", 14f))
                card.addView(label("Trạng thái đơn: $status", 14f))
                card.addView(button("📋 Sao chép mã") { copyCode(lucky) })
                root.addView(card)
            }
        }
        if (rewardCount == 0) {
            root.addView(label("Chưa có mã quay thưởng. Khi website cấp mã cho đơn hàng, mã sẽ tự xuất hiện tại đây sau khi đồng bộ.", 15f))
        }

        root.addView(button("📦 Đơn hàng của tôi") {
            startActivity(Intent(this, MainActivity::class.java).putExtra("screen", "orders"))
        })
        root.addView(button("🔄 Đồng bộ lại") { showAccount() })
        root.addView(button("🏠 Trang chủ") {
            startActivity(Intent(this, HomeActivity::class.java)); finish()
        })
    }

    private fun copyCode(code: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Mã quay thưởng COM11H", code))
        Toast.makeText(this, "Đã sao chép mã: $code", Toast.LENGTH_SHORT).show()
    }
}
