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
import java.util.concurrent.Executors

/** Standalone COM11H app. Only AccountSync communicates with com11h.com. */
class MainActivity : Activity() {
    private lateinit var account: AccountSync
    private val executor = Executors.newSingleThreadExecutor()
    private val primary = Color.rgb(245, 81, 30)
    private val dark = Color.rgb(38, 38, 38)
    private val secondary = Color.rgb(107, 107, 107)
    private val bgColor = Color.rgb(255, 248, 245)
    private val cart = linkedMapOf<Int, Int>()
    private val orders = mutableListOf<JSONObject>()

    data class Food(val id: Int, val name: String, val price: Int, val emoji: String, val desc: String, val rating: String)
    private val foods = listOf(
        Food(1, "Cơm sườn nướng", 45000, "🍖", "Sườn nướng thơm ngon", "4.9"),
        Food(2, "Cơm gà xối mỡ", 42000, "🍗", "Gà giòn rụm, đậm vị", "4.8"),
        Food(3, "Cơm bò lúc lắc", 48000, "🥩", "Bò mềm, sốt tiêu đen", "4.8"),
        Food(4, "Cơm cá kho tộ", 40000, "🐟", "Cá kho đậm đà, đưa cơm", "4.7"),
        Food(5, "Cơm thịt kho trứng", 43000, "🍳", "Món nhà thơm ngon", "4.8"),
        Food(6, "Cơm gà nướng mật ong", 46000, "🍗", "Thơm mềm, vị ngọt dịu", "4.9")
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Int) = String.format("%,d", v).replace(',', '.') + "đ"
    private fun bg(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun label(v: String, size: Float = 16f, color: Int = dark, bold: Boolean = false) = TextView(this).apply { text = v; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD); setPadding(0, dp(4), 0, dp(4)) }
    private fun button(v: String, click: () -> Unit) = TextView(this).apply { text = v; textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); background = bg(primary, 13); setPadding(dp(12), dp(13), dp(12), dp(13)); setOnClickListener { click() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); account = AccountSync(this); loadLocalData()
        when (intent.getStringExtra("screen")) { "menu" -> showMenu(); "cart" -> showCart(); "orders" -> showOrders(); "profile" -> showProfile(); else -> { startActivity(Intent(this, HomeActivity::class.java)); finish() } }
    }
    override fun onDestroy() { saveLocalData(); executor.shutdownNow(); super.onDestroy() }

    private fun loadLocalData() {
        val p = getSharedPreferences("com11h_local", MODE_PRIVATE); cart.clear(); orders.clear()
        try { val a = JSONArray(p.getString("cart", "[]")); for (i in 0 until a.length()) { val o = a.getJSONObject(i); cart[o.optInt("id")] = o.optInt("qty") }; val b = JSONArray(p.getString("orders", "[]")); for (i in 0 until b.length()) orders.add(b.getJSONObject(i)) } catch (_: Exception) { }
    }
    private fun saveLocalData() {
        val p = getSharedPreferences("com11h_local", MODE_PRIVATE); val a = JSONArray(); cart.forEach { (id, q) -> a.put(JSONObject().put("id", id).put("qty", q)) }; val b = JSONArray(); orders.forEach { b.put(it) }; p.edit().putString("cart", a.toString()).putString("orders", b.toString()).apply()
    }

    private fun shell(title: String, selected: Int): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bgColor) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)); setBackgroundColor(Color.WHITE) }
        header.addView(TextView(this).apply { text = "‹"; textSize = 34f; setTextColor(primary); gravity = Gravity.CENTER; setOnClickListener { startActivity(Intent(this@MainActivity, HomeActivity::class.java)); finish() } }, LinearLayout.LayoutParams(dp(42), dp(48)))
        header.addView(label(title, 19f, primary, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        header.addView(TextView(this).apply { text = "👤"; textSize = 19f; gravity = Gravity.CENTER; background = bg(Color.rgb(255, 240, 234), 22); setOnClickListener { showProfile() } }, LinearLayout.LayoutParams(dp(44), dp(44))); outer.addView(header)
        val scroll = ScrollView(this); val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(18)) }; scroll.addView(content); outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = dp(8).toFloat() }
        listOf("⌂\nTrang chủ", "▦\nThực đơn", "🛒\nGiỏ hàng", "▤\nĐơn hàng", "♙\nTài khoản").forEachIndexed { i, name -> nav.addView(TextView(this).apply { text = name; textSize = 10f; gravity = Gravity.CENTER; setTextColor(if (i == selected) primary else secondary); setTypeface(null, if (i == selected) Typeface.BOLD else Typeface.NORMAL); setOnClickListener { when (i) { 0 -> { startActivity(Intent(this@MainActivity, HomeActivity::class.java)); finish() }; 1 -> showMenu(); 2 -> showCart(); 3 -> showOrders(); 4 -> showProfile() } } }, LinearLayout.LayoutParams(0, dp(58), 1f)) }
        outer.addView(nav); return outer
    }
    private fun contentOf(s: LinearLayout) = (s.getChildAt(1) as ScrollView).getChildAt(0) as LinearLayout

    private fun showMenu() {
        val s = shell("Thực đơn", 1); setContentView(s); val c = contentOf(s); c.addView(label("Món ăn ngon mỗi ngày", 21f, dark, true)); c.addView(label("Dữ liệu món ăn độc lập trong app", 13f, secondary))
        foods.forEach { f ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = bg(Color.WHITE, 16); setPadding(dp(10), dp(10), dp(10), dp(10)); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) } }
            card.addView(TextView(this).apply { text = f.emoji; textSize = 40f; gravity = Gravity.CENTER; background = bg(Color.rgb(255,245,240), 14) }, LinearLayout.LayoutParams(dp(76), dp(76)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(6), 0) }; info.addView(label(f.name, 16f, dark, true)); info.addView(label(f.desc, 13f, secondary)); info.addView(label("${money(f.price)}   ⭐ ${f.rating}", 15f, primary, true)); card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(TextView(this).apply { text = "+"; textSize = 24f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = bg(primary, 22); setOnClickListener { cart[f.id] = (cart[f.id] ?: 0) + 1; saveLocalData(); Toast.makeText(this@MainActivity, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show() } }, LinearLayout.LayoutParams(dp(42), dp(42))); c.addView(card)
        }
    }

    private fun showCart() {
        val s = shell("Giỏ hàng", 2); setContentView(s); val c = contentOf(s); if (cart.isEmpty()) { c.addView(label("🛒 Giỏ hàng đang trống", 20f, dark, true)); c.addView(button("Xem thực đơn") { showMenu() }); return }
        var total = 0; cart.toMap().forEach { (id, qty) ->
            val f = foods.first { it.id == id }; total += f.price * qty; val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = bg(Color.WHITE, 14); setPadding(dp(10), dp(9), dp(10), dp(9)); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
            row.addView(TextView(this).apply { text = f.emoji; textSize = 30f }, LinearLayout.LayoutParams(dp(48), dp(48))); row.addView(label("${f.name}\n${money(f.price)}", 15f, dark, true), LinearLayout.LayoutParams(0, -2, 1f)); row.addView(TextView(this).apply { text = "−  $qty  +"; textSize = 14f; setTextColor(primary); setOnClickListener { cart[id] = qty + 1; saveLocalData(); showCart() } }); c.addView(row)
        }
        c.addView(label("Tổng cộng: ${money(total)}", 20f, primary, true).apply { gravity = Gravity.END; setPadding(0, dp(12), 0, dp(12)) }); c.addView(button("Đặt hàng ${money(total)}") { val order = JSONObject().put("code", "DH${System.currentTimeMillis().toString().takeLast(8)}").put("total", total).put("status", "Đã đặt").put("items", cart.values.sum()); orders.add(0, order); cart.clear(); saveLocalData(); Toast.makeText(this, "Đặt hàng thành công", Toast.LENGTH_LONG).show(); showOrders() })
    }

    private fun showOrders() {
        val s = shell("Đơn hàng", 3); setContentView(s); val c = contentOf(s); if (orders.isEmpty()) { c.addView(label("📦 Chưa có đơn hàng", 20f, dark, true)); c.addView(label("Đơn hàng của ứng dụng được lưu độc lập và không gửi sang website.")); return }
        orders.forEach { o -> val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = bg(Color.WHITE, 15); setPadding(dp(14), dp(12), dp(14), dp(12)); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) } }; card.addView(label("#${o.optString("code")}", 16f, dark, true)); card.addView(label("${o.optInt("items")} món • ${money(o.optInt("total"))}", 15f, primary, true)); card.addView(label("Trạng thái: ${o.optString("status")}", 14f, secondary)); c.addView(card) }
    }

    private fun showProfile() {
        val s = shell("Tài khoản", 4); setContentView(s); val c = contentOf(s)
        if (!account.isLoggedIn()) { c.addView(label("👤 Tài khoản khách hàng", 22f, dark, true)); c.addView(label("Đăng nhập để đồng bộ tài khoản với tài khoản COM11H đang dùng trên website.")); c.addView(button("🔐 Đăng nhập / Đăng ký") { showLogin() }); return }
        c.addView(label("👤 Tài khoản khách hàng", 22f, dark, true)); val loading = label("Đang đồng bộ thông tin tài khoản...", 15f, secondary); c.addView(loading)
        executor.execute { try { val r = account.request("profile"); runOnUiThread { c.removeView(loading); if (r.optBoolean("ok")) { val customer = r.optJSONObject("data")?.optJSONObject("customer") ?: r.optJSONObject("data") ?: JSONObject(); c.addView(label("Họ tên: ${customer.optString("name", "Chưa cập nhật")}", 17f, dark, true)); c.addView(label("Số điện thoại: ${customer.optString("phone", "")}")); c.addView(label("Điểm tích lũy: ${customer.optInt("points", 0)} điểm", 18f, primary, true)); c.addView(label("Thông tin trên được đồng bộ từ tài khoản COM11H hiện có.")); c.addView(button("🔄 Đồng bộ lại") { showProfile() }); c.addView(button("🚪 Đăng xuất") { account.logout(); showProfile() }) } else { account.logout(); c.addView(label(r.optString("message", "Phiên đăng nhập đã hết hạn."))); c.addView(button("Đăng nhập lại") { showLogin() }) } } } catch (_: Exception) { runOnUiThread { loading.text = "Không thể đồng bộ tài khoản. Kiểm tra mạng rồi thử lại."; c.addView(button("Thử lại") { showProfile() }) } } }
    }

    private fun input(hint: String, password: Boolean = false) = EditText(this).apply { this.hint = hint; textSize = 16f; setPadding(dp(12), dp(10), dp(12), dp(10)); if (password) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
    private fun showLogin() {
        val s = shell("Đăng nhập", 4); setContentView(s); val c = contentOf(s); val phone = input("Số điện thoại"); val pass = input("Mật khẩu", true); c.addView(phone); c.addView(pass)
        c.addView(button("Đăng nhập") { val p = phone.text.toString().trim(); val pw = pass.text.toString(); if (p.isBlank() || pw.isBlank()) { Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show(); return@button }; executor.execute { try { val r = account.request("login", "POST", JSONObject(mapOf("phone" to p, "password" to pw, "device" to "COM11H Android Standalone")).toString()); runOnUiThread { if (r.optBoolean("ok")) { account.saveToken(r.optJSONObject("data")?.optString("token", "") ?: ""); Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show(); showProfile() } else Toast.makeText(this, r.optString("message", "Đăng nhập thất bại"), Toast.LENGTH_SHORT).show() } } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Không kết nối được máy chủ tài khoản", Toast.LENGTH_SHORT).show() } } } })
        c.addView(button("Đăng ký tài khoản mới") { showRegister() })
    }
    private fun showRegister() {
        val s = shell("Đăng ký tài khoản", 4); setContentView(s); val c = contentOf(s); val name = input("Họ tên"); val phone = input("Số điện thoại"); val pass = input("Mật khẩu", true); val pass2 = input("Nhập lại mật khẩu", true); c.addView(name); c.addView(phone); c.addView(pass); c.addView(pass2)
        c.addView(button("Tạo tài khoản") { if (name.text.isBlank() || phone.text.isBlank() || pass.text.length < 6 || pass.text.toString() != pass2.text.toString()) { Toast.makeText(this, "Kiểm tra lại thông tin đăng ký", Toast.LENGTH_SHORT).show(); return@button }; executor.execute { try { val body = JSONObject(mapOf("name" to name.text.toString().trim(), "phone" to phone.text.toString().trim(), "password" to pass.text.toString(), "password2" to pass2.text.toString(), "device" to "COM11H Android Standalone")).toString(); val r = account.request("register", "POST", body); runOnUiThread { if (r.optBoolean("ok")) { account.saveToken(r.optJSONObject("data")?.optString("token", "") ?: ""); Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show(); showProfile() } else Toast.makeText(this, r.optString("message", "Đăng ký thất bại"), Toast.LENGTH_SHORT).show() } } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Không kết nối được máy chủ tài khoản", Toast.LENGTH_SHORT).show() } } } })
    }
}
