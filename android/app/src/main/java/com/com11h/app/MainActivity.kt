package com.com11h.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import android.graphics.drawable.GradientDrawable
import android.view.animation.Animation
import android.view.animation.TranslateAnimation

private class Api(private val context: Context) {
    private val baseUrl = "https://com11h.com/api/index.php"
    private val prefs = context.getSharedPreferences("com11h_secure", Context.MODE_PRIVATE)

    fun token(): String? = prefs.getString("token", null)
    fun saveToken(token: String) = prefs.edit().putString("token", token).apply()
    fun clearToken() = prefs.edit().remove("token").apply()
    fun hasToken(): Boolean = !token().isNullOrBlank()

    fun request(
        action: String,
        method: String = "GET",
        body: String? = null,
        params: Map<String, String> = emptyMap(),
        idempotencyKey: String? = null
    ): JSONObject {
        val query = mutableListOf("action=${URLEncoder.encode(action, "UTF-8")}")
        params.forEach { (k, v) ->
            query += "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val conn = (URL("$baseUrl?${query.joinToString("&")}").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 15000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            token()?.let { setRequestProperty("Authorization", "Bearer $it") }
            idempotencyKey?.let { setRequestProperty("X-Idempotency-Key", it) }
        }

        return try {
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = try { JSONObject(text) } catch (_: Exception) {
                JSONObject().put("ok", false).put("message", "Máy chủ trả về dữ liệu không hợp lệ (HTTP $code).")
            }
            if (!json.has("http_code")) json.put("http_code", code)
            json
        } finally {
            conn.disconnect()
        }
    }
}

class MainActivity : Activity() {
    private lateinit var api: Api
    private lateinit var root: LinearLayout
    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var paymentPolling = false
    private val cart = linkedMapOf<Int, Int>()
    private val foodCache = hashMapOf<Int, JSONObject>()

    // Cụm nút nổi cố định (Giỏ hàng + Trang chủ) neo góc dưới-trái, hiển thị xuyên suốt mọi màn hình.
    private lateinit var cartFabContainer: LinearLayout
    private lateinit var cartFabLabel: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Int) = String.format("%,d", v).replace(',', '.') + "đ"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = Api(this)
        loadCart()
        when (intent.getStringExtra("screen")) {
            "cart" -> showCart()
            "profile" -> showProfile()
            "orders" -> showOrders()
            else -> showHome()
        }
    }

    override fun onDestroy() {
        saveCart()
        paymentPolling = false
        mainHandler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun saveCart() {
        val arr = JSONArray()
        cart.forEach { (id, qty) -> arr.put(JSONObject().put("id", id).put("qty", qty)) }
        getSharedPreferences("com11h_secure", MODE_PRIVATE).edit().putString("cart", arr.toString()).apply()
    }

    private fun loadCart() {
        cart.clear()
        val raw = getSharedPreferences("com11h_secure", MODE_PRIVATE).getString("cart", null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optInt("id")
                val qty = o.optInt("qty")
                if (id > 0 && qty > 0) cart[id] = qty
            }
        } catch (_: Exception) { }
    }

    private fun setup(title: String) {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
            setBackgroundColor(Color.rgb(248, 250, 248))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Chừa khoảng trống dưới cùng để nội dung không bị cụm nút nổi (Giỏ hàng/Trang chủ) che mất.
            setPadding(0, 0, 0, dp(140))
        }
        val bar = TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(Color.rgb(22, 128, 60))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(bar)
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root = content

        // Lớp phủ FrameLayout: nội dung trang + cụm nút nổi (Giỏ hàng/Trang chủ) neo cố định góc dưới-trái.
        val overlay = FrameLayout(this)
        overlay.addView(page, FrameLayout.LayoutParams(-1, -1))
        overlay.addView(
            buildFloatingNav(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(dp(14), 0, 0, dp(16))
            }
        )
        setContentView(overlay)
    }

    /** Cụm nút nổi cố định góc dưới-trái: Giỏ hàng (trên) + Trang chủ (dưới), đúng vị trí mặc định người dùng chọn. */
    private fun buildFloatingNav(): LinearLayout {
        val cluster = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun pill(bg: Int): GradientDrawable = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(bg)
        }

        cartFabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(18), dp(10))
            background = pill(Color.rgb(22, 128, 60))
            elevation = dp(6).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { showCart() }
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(10) }
        }
        cartFabContainer.addView(TextView(this).apply { text = "🛒"; textSize = 20f })
        cartFabLabel = TextView(this).apply {
            text = "  Giỏ hàng (${cart.values.sum()})"
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        cartFabContainer.addView(cartFabLabel)
        cluster.addView(cartFabContainer)

        val homeFab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(18), dp(10))
            background = pill(Color.WHITE)
            elevation = dp(6).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { showHome() }
        }
        homeFab.addView(TextView(this).apply { text = "🏠"; textSize = 18f })
        homeFab.addView(TextView(this).apply {
            text = "  Trang chủ"
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(22, 128, 60))
        })
        cluster.addView(homeFab)

        return cluster
    }

    /** Cập nhật số lượng trên nút Giỏ hàng nổi + rung lắc để khách thấy sinh động khi vừa thêm món. */
    private fun updateCartFab() {
        if (!::cartFabLabel.isInitialized) return
        cartFabLabel.text = "  Giỏ hàng (${cart.values.sum()})"
        shakeView(cartFabContainer)
    }

    private fun shakeView(view: View) {
        val anim = TranslateAnimation(-dp(10).toFloat(), dp(10).toFloat(), 0f, 0f).apply {
            duration = 70
            repeatCount = 5
            repeatMode = Animation.REVERSE
        }
        view.startAnimation(anim)
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 16f
        minimumHeight = dp(48)
        setOnClickListener { action() }
    }

    private fun input(hint: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint
        textSize = 16f
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun showHome() {
        setup("COM11H 🍚")
        root.addView(TextView(this).apply {
            text = "Cơm trưa ngon – đặt nhanh – giao tận nơi"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(22))
        })
        root.addView(button("🍱  Xem thực đơn") { showMenu() })
        root.addView(button("📦  Đơn hàng của tôi") { showOrders() })
        root.addView(button("👤  Tài khoản") { showProfile() })
        if (api.token() == null) root.addView(button("🔐  Đăng nhập / Đăng ký") { showLogin() })
        else root.addView(button("🚪  Đăng xuất") { logoutAndHome() })
    }

    private fun showLogin() {
        setup("Đăng nhập")
        val phone = input("Số điện thoại").apply { inputType = InputType.TYPE_CLASS_PHONE }
        val pass = input("Mật khẩu", true)
        root.addView(phone); root.addView(pass)
        lateinit var login: Button
        login = button("Đăng nhập") {
            val p = phone.text.toString().trim()
            val pw = pass.text.toString()
            if (p.isEmpty() || pw.isEmpty()) { toast("Vui lòng nhập số điện thoại và mật khẩu"); return@button }
            login.isEnabled = false
            executor.execute {
                try {
                    val r = api.request("login", "POST", JSONObject(mapOf("phone" to p, "password" to pw, "device" to "COM11H Android")).toString())
                    runOnUiThread {
                        login.isEnabled = true
                        if (r.optBoolean("ok")) {
                            api.saveToken(r.getJSONObject("data").getString("token"))
                            toast("Đăng nhập thành công")
                            showHome()
                        } else toast(r.optString("message", "Đăng nhập thất bại"))
                    }
                } catch (e: Exception) { runOnUiThread { login.isEnabled = true; toast("Lỗi kết nối: ${e.message ?: "không xác định"}") } }
            }
        }
        root.addView(login)
        root.addView(button("Đăng ký tài khoản") { showRegister() })
        root.addView(button("← Quay lại") { showHome() })
    }

    private fun showRegister() {
        setup("Đăng ký tài khoản")
        val name = input("Họ tên")
        val phone = input("Số điện thoại").apply { inputType = InputType.TYPE_CLASS_PHONE }
        val pass = input("Mật khẩu – tối thiểu 6 ký tự", true)
        val pass2 = input("Nhập lại mật khẩu", true)
        root.addView(name); root.addView(phone); root.addView(pass); root.addView(pass2)
        lateinit var register: Button
        register = button("Tạo tài khoản") {
            val n = name.text.toString().trim(); val p = phone.text.toString().trim(); val pw = pass.text.toString(); val pw2 = pass2.text.toString()
            if (n.isEmpty() || p.isEmpty() || pw.isEmpty()) { toast("Vui lòng nhập đầy đủ thông tin"); return@button }
            if (pw.length < 6) { toast("Mật khẩu tối thiểu 6 ký tự"); return@button }
            if (pw != pw2) { toast("Mật khẩu nhập lại không khớp"); return@button }
            register.isEnabled = false
            executor.execute {
                try {
                    val body = JSONObject(mapOf("name" to n, "phone" to p, "password" to pw, "password2" to pw2, "device" to "COM11H Android")).toString()
                    val r = api.request("register", "POST", body)
                    runOnUiThread {
                        register.isEnabled = true
                        if (r.optBoolean("ok")) {
                            api.saveToken(r.getJSONObject("data").getString("token"))
                            toast("Đăng ký thành công")
                            showHome()
                        } else toast(r.optString("message", "Đăng ký thất bại"))
                    }
                } catch (e: Exception) { runOnUiThread { register.isEnabled = true; toast("Lỗi kết nối: ${e.message ?: "không xác định"}") } }
            }
        }
        root.addView(register)
        root.addView(button("← Quay lại") { showLogin() })
    }

    private fun showMenu() {
        setup("Thực đơn")
        val loading = TextView(this).apply { text = "Đang tải thực đơn..."; textSize = 17f }
        root.addView(loading)
        executor.execute {
            try {
                val r = api.request("menu")
                if (!r.optBoolean("ok")) throw IllegalStateException(r.optString("message"))
                val data = r.getJSONObject("data")
                val foods = data.getJSONArray("foods")
                runOnUiThread {
                    root.removeView(loading)
                    if (foods.length() == 0) root.addView(TextView(this).apply { text = "Hiện chưa có món."; textSize = 17f })
                    for (i in 0 until foods.length()) {
                        val f = foods.getJSONObject(i)
                        foodCache[f.getInt("id")] = f
                        root.addView(foodCard(f))
                    }
                }
            } catch (e: Exception) { runOnUiThread { loading.text = "Không tải được thực đơn: ${e.message ?: "Vui lòng thử lại"}" } }
        }
    }

    /** 1 thẻ món ăn: ảnh bên trái (có logo nhỏ góc trên-trái, bấm để xem ảnh lớn)
     *  + tên/giá/kho/mô tả bên phải + nút thêm vào giỏ (chữ nhỏ, nền vàng nhạt). */
    private fun foodCard(f: JSONObject): View {
        val stock = f.optInt("stock", 0)
        val desc = f.optString("description")
        val imageUrl = f.optString("image")
        val foodName = f.optString("name")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(10)) }
        }

        // Ô ảnh: to hơn trước (128dp thay vì 88dp), có logo nhỏ góc trên-trái,
        // bấm vào ảnh sẽ mở xem ảnh lớn.
        val imageBox = FrameLayout(this)
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(238, 238, 238))
            isClickable = true
            setOnClickListener { if (imageUrl.isNotBlank()) showImagePreview(imageUrl, foodName) }
        }
        imageBox.addView(thumb, FrameLayout.LayoutParams(dp(128), dp(128)))
        if (imageUrl.isNotBlank()) loadImageInto(imageUrl, thumb)

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.com11h_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        imageBox.addView(
            logo,
            FrameLayout.LayoutParams(dp(26), dp(26)).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(dp(4), dp(4), 0, 0)
            }
        )
        card.addView(imageBox)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        info.addView(TextView(this).apply {
            text = foodName; textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD)
        })
        info.addView(TextView(this).apply {
            text = money(f.optInt("price")) + "  •  Kho: $stock"; textSize = 14f
        })
        if (desc.isNotBlank()) {
            info.addView(TextView(this).apply {
                text = desc; textSize = 13f; setTextColor(Color.DKGRAY); maxLines = 2
            })
        }
        // Hàng chứa nút "+ THÊM": ô nút chiếm đúng 50% bề ngang và nằm bên phải (nửa trái để trống).
        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        }
        addRow.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f)) // nửa trái để trống
        addRow.addView(Button(this).apply {
            text = if (stock <= 0) "Hết hàng" else "+ THÊM"
            textSize = 12f
            minimumHeight = dp(38)
            setPadding(dp(4), 0, dp(4), 0)
            setBackgroundColor(Color.rgb(255, 244, 179)) // vàng nhạt
            setTextColor(Color.rgb(80, 60, 0))
            isEnabled = stock > 0
            setOnClickListener { if (stock <= 0) toast("Món này đã hết") else addFood(f) }
        }, LinearLayout.LayoutParams(0, dp(38), 1f)) // nửa phải: đúng 50% bề ngang
        info.addView(addRow)

        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        return card
    }

    /** Mở ảnh món ăn xem full-size (bấm ra ngoài hoặc bấm ảnh để đóng). */
    private fun showImagePreview(imageUrl: String, title: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val big = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(big, LinearLayout.LayoutParams(-1, dp(320)))
        loadImageInto(imageUrl, big)

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Đóng", null)
            .create()
        big.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addFood(food: JSONObject) {
        val id = food.getInt("id")
        val stock = food.optInt("stock", 0)
        val current = cart[id] ?: 0
        if (current >= stock) { toast("Số lượng đã đạt tồn kho hiện tại"); return }
        cart[id] = current + 1
        foodCache[id] = food
        saveCart()
        toast("Đã thêm ${food.optString("name")}")
        updateCartFab()
    }

    /** 1 dòng giỏ hàng: bố cục giống thẻ món ở Thực đơn — ảnh bên trái, tên/đơn giá bên phải,
     *  và bộ đếm số lượng (−  qty  +) nhỏ gọn thay cho nút hệ thống to bản gây vỡ layout. */
    private fun cartRow(id: Int, qty: Int, f: JSONObject): View {
        val price = f.optInt("price")
        val stock = f.optInt("stock", 0)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(10)) }
        }

        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(238, 238, 238))
        }
        card.addView(thumb, LinearLayout.LayoutParams(dp(72), dp(72)))
        val imgUrl = f.optString("image")
        if (imgUrl.isNotBlank()) loadImageInto(imgUrl, thumb)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(6), 0)
        }
        info.addView(TextView(this).apply {
            text = f.optString("name")
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        info.addView(TextView(this).apply {
            text = "${money(price)} × $qty = ${money(price * qty)}"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

        fun stepBtn(label: String, action: () -> Unit) = TextView(this).apply {
            text = label
            textSize = 17f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(22, 128, 60))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.rgb(238, 238, 238))
            }
            isClickable = true
            setOnClickListener { action() }
        }

        val stepper = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        stepper.addView(
            stepBtn("−") { if ((cart[id] ?: 0) <= 1) cart.remove(id) else cart[id] = (cart[id] ?: 1) - 1; saveCart(); showCart() },
            LinearLayout.LayoutParams(dp(32), dp(32))
        )
        stepper.addView(
            TextView(this).apply { text = "$qty"; textSize = 15f; gravity = Gravity.CENTER; setTypeface(null, android.graphics.Typeface.BOLD) },
            LinearLayout.LayoutParams(dp(30), dp(32))
        )
        stepper.addView(
            stepBtn("+") {
                val current = cart[id] ?: 0
                if (current >= stock) toast("Đã đạt tồn kho hiện tại") else { cart[id] = current + 1; saveCart(); showCart() }
            },
            LinearLayout.LayoutParams(dp(32), dp(32))
        )
        card.addView(stepper)

        return card
    }

    private fun showCart() {
        setup("Giỏ hàng")
        if (cart.isEmpty()) {
            root.addView(TextView(this).apply { text = "Giỏ hàng đang trống."; textSize = 18f })
        } else {
            var total = 0
            cart.toMap().forEach { (id, qty) ->
                val f = foodCache[id]
                if (f != null) {
                    total += f.optInt("price") * qty
                    root.addView(cartRow(id, qty, f))
                }
            }
            root.addView(TextView(this).apply { text = "Tạm tính: ${money(total)}\nGiá cuối cùng sẽ được máy chủ kiểm tra lại trước khi đặt."; textSize = 18f; setPadding(0, dp(16), 0, dp(16)) })
            if (api.token() == null) root.addView(button("🔐 Đăng nhập để đặt hàng") { showLogin() })
            else root.addView(button("📦 Tiến hành đặt hàng") { showCheckout() })
        }
        root.addView(button("← Thực đơn") { showMenu() })
    }

    private fun showCheckout() {
        if (api.token() == null) { showLogin(); return }
        if (cart.isEmpty()) { toast("Giỏ hàng đang trống"); showCart(); return }
        setup("Xác nhận đặt hàng")
        val address = input("Địa chỉ giao hàng *")
        val delivery = input("Thời gian giao (tuỳ chọn)")
        val note = input("Ghi chú")
        root.addView(address); root.addView(delivery); root.addView(note)

        lateinit var orderButton: Button
        orderButton = button("🔎 Kiểm tra đơn hàng") {
            val addr = address.text.toString().trim()
            if (addr.isEmpty()) { toast("Vui lòng nhập địa chỉ giao hàng"); return@button }
            orderButton.isEnabled = false
            val arr = cartJson()
            val body = JSONObject().apply { put("items", arr); put("address", addr); put("delivery_time", delivery.text.toString().trim()); put("note", note.text.toString().trim()) }
            executor.execute {
                try {
                    val r = api.request("order_preview", "POST", body.toString())
                    runOnUiThread {
                        orderButton.isEnabled = true
                        if (r.optBoolean("ok")) {
                            val d = r.getJSONObject("data")
                            val total = d.optInt("total")
                            AlertDialog.Builder(this)
                                .setTitle("Kiểm tra đơn hàng")
                                .setMessage("Tổng tiền máy chủ xác nhận: ${money(total)}\n\nĐịa chỉ: $addr\n\nBấm ĐẶT HÀNG để tạo đơn thật.")
                                .setNegativeButton("Sửa lại", null)
                                .setPositiveButton("ĐẶT HÀNG") { _, _ -> createOrder(body) }
                                .show()
                        } else toast(r.optString("message", "Không thể kiểm tra đơn hàng"))
                    }
                } catch (_: Exception) { runOnUiThread { orderButton.isEnabled = true; toast("Không kết nối được máy chủ") } }
            }
        }
        root.addView(orderButton)
        root.addView(button("← Giỏ hàng") { showCart() })
    }

    private fun cartJson(): JSONArray {
        val arr = JSONArray()
        cart.forEach { (id, qty) -> arr.put(JSONObject().put("food_id", id).put("qty", qty)) }
        return arr
    }

    private fun createOrder(body: JSONObject) {
        // Deterministic idempotency key: the same order payload gets the same key.
        // This prevents duplicate orders when the network times out and the user retries.
        val key = idempotencyKeyFor(body.toString())
        executor.execute {
            try {
                val r = api.request("create_order", "POST", body.toString(), idempotencyKey = key)
                runOnUiThread {
                    if (r.optBoolean("ok")) {
                        cart.clear(); saveCart()
                        showPayment(r.getJSONObject("data"))
                    } else toast(r.optString("message", "Không thể tạo đơn hàng"))
                }
            } catch (_: Exception) {
                runOnUiThread {
                    toast("Mạng chập chờn. Bạn có thể thử lại; hệ thống sẽ chống tạo trùng đơn.")
                }
            }
        }
    }

    private fun idempotencyKeyFor(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun showPayment(data: JSONObject) {
        paymentPolling = false
        mainHandler.removeCallbacksAndMessages(null)
        setup("Thanh toán đơn hàng")
        val order = data.getJSONObject("order")
        val p = data.getJSONObject("payment")
        val code = order.getString("code")
        val total = order.getInt("total")
        val statusView = TextView(this).apply {
            text = "⏳ Đang chờ xác nhận thanh toán...\nMã đơn: $code"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(statusView)
        root.addView(TextView(this).apply {
            text = "Mã đơn: $code\nSố tiền: ${money(total)}\nNgân hàng: ${p.optString("bank_display_name", p.optString("bank_name"))}\nSTK: ${p.optString("bank_account_no", p.optString("bank_account"))}\nChủ TK: ${p.optString("bank_account_name", p.optString("account_name"))}\nNội dung: ${p.optString("transfer_content")}"
            textSize = 17f
            setPadding(0, 0, 0, dp(14))
        })

        // Gói QR trong 1 container riêng để có thể ẩn cả cụm ngay khi thanh toán được xác nhận.
        val qrContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val qr = ImageView(this).apply { adjustViewBounds = true; minimumHeight = dp(220); scaleType = ImageView.ScaleType.CENTER_INSIDE }
        qrContainer.addView(qr, LinearLayout.LayoutParams(-1, dp(260)))
        root.addView(qrContainer)
        loadImageInto(p.optString("qr_url"), qr)

        val openQrButton = button("📱 Mở QR bằng trình duyệt/app ngân hàng") { openUrl(p.optString("qr_url")) }
        root.addView(openQrButton)
        root.addView(button("🔄 Kiểm tra ngay") { showOrder(code) })
        root.addView(button("← Trang chủ") { showHome() })

        startPaymentPolling(code, statusView, qrContainer, openQrButton)
    }

    private fun startPaymentPolling(code: String, statusView: TextView, qrContainer: View, openQrButton: View) {
        paymentPolling = true
        val startedAt = System.currentTimeMillis()
        val maxDurationMs = 10 * 60 * 1000L

        fun poll() {
            if (!paymentPolling || isFinishing || isDestroyed) return
            if (System.currentTimeMillis() - startedAt >= maxDurationMs) {
                paymentPolling = false
                statusView.text = "⏳ Chưa nhận được xác nhận thanh toán. Bạn có thể bấm kiểm tra lại."
                return
            }
            executor.execute {
                var paid = false
                try {
                    val r = api.request("order", params = mapOf("code" to code))
                    val d = r.optJSONObject("data")
                    val o = d?.optJSONObject("order")
                    paid = o?.optString("payment_status") == "paid"
                } catch (_: Exception) {
                    // Keep polling silently; the user can still use "Kiểm tra ngay".
                }

                if (paid) {
                    // Dừng polling ở background trước khi đụng UI, tránh 1 vòng poll() khác
                    // đang chạy song song lại đặt lịch thêm 1 lần nữa.
                    paymentPolling = false

                    // Làm mới thông tin tài khoản (điểm) ngay để lần sau vào "Tài khoản" đã cập nhật.
                    try { api.request("profile") } catch (_: Exception) { }

                    runOnUiThread {
                        qrContainer.visibility = View.GONE
                        openQrButton.visibility = View.GONE
                        statusView.text = "✅ Đã thanh toán\nMã đơn: $code"
                        toast("Thanh toán đơn $code đã được xác nhận")
                        mainHandler.postDelayed({ showOrder(code) }, 900)
                    }
                } else {
                    runOnUiThread {
                        if (paymentPolling) statusView.text = "⏳ Đang chờ ngân hàng xác nhận...\nMã đơn: $code\nTự kiểm tra mỗi 5 giây"
                    }
                    if (paymentPolling) mainHandler.postDelayed({ poll() }, 5000L)
                }
            }
        }
        mainHandler.post { poll() }
    }

    private fun showOrders() {
        if (api.token() == null) { showLogin(); return }
        setup("Đơn hàng của tôi")
        val loading = TextView(this).apply { text = "Đang tải..."; textSize = 17f }; root.addView(loading)
        executor.execute {
            try {
                val r = api.request("orders")
                runOnUiThread {
                    if (!r.optBoolean("ok")) { loading.text = r.optString("message", "Không tải được đơn hàng"); return@runOnUiThread }
                    val a = r.getJSONObject("data").getJSONArray("orders")
                    root.removeView(loading)
                    if (a.length() == 0) root.addView(TextView(this).apply { text = "Chưa có đơn hàng."; textSize = 17f })
                    for (i in 0 until a.length()) {
                        root.addView(orderCard(a.getJSONObject(i)))
                    }
                    root.addView(button("🔄 Làm mới") { showOrders() })
                    root.addView(button("← Trang chủ") { showHome() })
                }
            } catch (_: Exception) { runOnUiThread { loading.text = "Không tải được đơn hàng. Vui lòng thử lại." } }
        }
    }

    /** 1 thẻ đơn hàng trong danh sách — đủ 6 cột như bảng account.php trên web:
     *  Mã đơn (+ngày), Tổng tiền, Thanh toán, Trạng thái, Quay thưởng, Nhận hàng. */
    private fun orderCard(o: JSONObject): View {
        val code = o.getString("code")
        val createdAt = o.optString("created_at")
        val total = o.optInt("total")
        val paid = o.optString("payment_status") == "paid"
        val status = o.optString("status")
        val luckyCode = o.optString("lucky_code")
        val confirmed = o.optInt("delivery_confirmed") == 1
        val points = o.optInt("points_earned")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) }
            isClickable = true
            setOnClickListener { showOrder(code) }
        }

        card.addView(TextView(this).apply {
            text = "Mã đơn: $code" + if (createdAt.isNotBlank()) "\n$createdAt" else ""
            textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD)
        })
        card.addView(row("Tổng tiền", money(total)))
        card.addView(row("Thanh toán", if (paid) "✅ Đã thanh toán" else "⏳ Chưa thanh toán"))
        card.addView(row("Trạng thái", status + if (confirmed) "\nKhách đã xác nhận nhận hàng" else ""))
        card.addView(row("Quay thưởng", if (luckyCode.isNotBlank()) luckyCode else "—"))

        // Cột "Nhận hàng"
        when {
            confirmed -> card.addView(row("Nhận hàng", "✅ Đã nhận hàng  •  +$points điểm"))
            status == "Hoàn thành" -> {
                lateinit var confirmBtn: Button
                confirmBtn = button("📦 Tôi đã nhận hàng") {
                    confirmBtn.isEnabled = false
                    executor.execute {
                        try {
                            val rr = api.request("confirm_delivery", "POST", JSONObject().put("code", code).toString())
                            runOnUiThread {
                                if (rr.optBoolean("ok")) { toast(rr.optString("message", "Đã xác nhận nhận hàng")); showOrders() }
                                else { confirmBtn.isEnabled = true; toast(rr.optString("message", "Chưa thể xác nhận nhận hàng")) }
                            }
                        } catch (_: Exception) { runOnUiThread { confirmBtn.isEnabled = true; toast("Không kết nối được máy chủ") } }
                    }
                }
                card.addView(confirmBtn)
            }
            status == "Đang giao" -> card.addView(row("Nhận hàng", "🚚 Đang giao. Nút xác nhận sẽ xuất hiện khi đơn Hoàn thành."))
            else -> card.addView(row("Nhận hàng", "Chưa thể xác nhận nhận hàng."))
        }

        return card
    }

    private fun row(label: String, value: String) = TextView(this).apply {
        text = "$label: $value"; textSize = 15f; setPadding(0, dp(4), 0, dp(4))
    }

    private fun showOrder(code: String) {
        setup("Chi tiết đơn $code")
        val loading = TextView(this).apply { text = "Đang tải đơn..."; textSize = 17f }
        root.addView(loading)
        executor.execute {
            try {
                val r = api.request("order", params = mapOf("code" to code))
                runOnUiThread {
                    root.removeView(loading)
                    if (!r.optBoolean("ok")) { root.addView(TextView(this).apply { text = r.optString("message", "Không tải được đơn"); textSize = 17f }); return@runOnUiThread }
                    val d = r.getJSONObject("data")
                    val o = d.getJSONObject("order")
                    val items = d.getJSONArray("items")
                    val status = o.optString("status")
                    val paid = o.optString("payment_status") == "paid"
                    val confirmed = o.optInt("delivery_confirmed") == 1
                    val points = o.optInt("points_earned")
                    val lucky = o.optString("lucky_code", "")

                    root.addView(TextView(this).apply {
                        text = "Trạng thái: $status\nThanh toán: ${if (paid) "Đã thanh toán" else "Chưa thanh toán"}\nTổng: ${money(o.getInt("total"))}\nĐịa chỉ: ${o.optString("address")}\nGiao: ${o.optString("delivery_time")}" + if (o.optString("note").isNotBlank()) "\nGhi chú: ${o.optString("note")}" else ""
                        textSize = 17f
                    })

                    root.addView(TextView(this).apply { text = "\nTiến trình đơn hàng"; textSize = 19f; setTypeface(null, 1) })
                    listOf("Chờ xác nhận", "Đã xác nhận", "Đang nấu", "Đang giao", "Hoàn thành").forEach { s ->
                        root.addView(TextView(this).apply { text = if (statusRank(status) >= statusRank(s)) "✓ $s" else "○ $s"; textSize = 16f; setPadding(dp(8), dp(3), 0, dp(3)) })
                    }

                    root.addView(TextView(this).apply { text = "\nMón đã đặt"; textSize = 19f; setTypeface(null, 1) })
                    for (i in 0 until items.length()) {
                        val it = items.getJSONObject(i)
                        root.addView(TextView(this).apply { text = "• ${it.optString("name")} × ${it.optInt("qty")} = ${money(it.optInt("price") * it.optInt("qty"))}"; textSize = 16f; setPadding(0, dp(6), 0, dp(6)) })
                    }

                    if (lucky.isNotBlank()) root.addView(TextView(this).apply { text = "\n🎁 Mã dự thưởng: $lucky"; textSize = 20f; setTypeface(null, 1) })
                    if (confirmed) root.addView(TextView(this).apply { text = "\n✅ Bạn đã xác nhận nhận hàng\n⭐ Đã cộng: +$points điểm"; textSize = 18f })

                    val p = d.optJSONObject("payment")
                    if (p != null && !paid) {
                        root.addView(button("📱 Mở QR thanh toán") { openUrl(p.optString("qr_url")) })
                        val qr = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_INSIDE }
                        root.addView(qr, LinearLayout.LayoutParams(-1, dp(250)))
                        loadImageInto(p.optString("qr_url"), qr)
                    }

                    if (status == "Hoàn thành" && !confirmed) {
                        lateinit var confirm: Button
                        confirm = button("✅ Tôi đã nhận hàng") {
                            confirm.isEnabled = false
                            executor.execute {
                                try {
                                    val rr = api.request("confirm_delivery", "POST", JSONObject().put("code", code).toString())
                                    runOnUiThread {
                                        confirm.isEnabled = true
                                        if (rr.optBoolean("ok")) {
                                            toast(rr.optString("message", "Đã xác nhận nhận hàng"))
                                            showOrder(code)
                                        } else toast(rr.optString("message", "Chưa thể xác nhận nhận hàng"))
                                    }
                                } catch (_: Exception) { runOnUiThread { confirm.isEnabled = true; toast("Không kết nối được máy chủ") } }
                            }
                        }
                        root.addView(confirm)
                    }

                    root.addView(button("🔄 Làm mới") { showOrder(code) })
                    root.addView(button("← Đơn hàng") { showOrders() })
                    root.addView(button("← Trang chủ") { showHome() })
                }
            } catch (_: Exception) { runOnUiThread { loading.text = "Không tải được chi tiết đơn. Vui lòng thử lại." } }
        }
    }

    private fun statusRank(status: String): Int = when (status) {
        "Chờ xác nhận" -> 0
        "Đã xác nhận" -> 1
        "Đang nấu" -> 2
        "Đang giao" -> 3
        "Hoàn thành" -> 4
        else -> -1
    }

    private fun logoutAndHome() {
        if (!api.hasToken()) { api.clearToken(); showHome(); return }
        executor.execute {
            try { api.request("logout", "POST") } catch (_: Exception) { }
            runOnUiThread { api.clearToken(); showHome() }
        }
    }

    private fun deleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Xóa tài khoản")
            .setMessage("Tài khoản và dữ liệu định danh sẽ được xử lý xóa. Lịch sử giao dịch cần lưu để đối soát có thể được ẩn danh. Bạn có chắc chắn muốn tiếp tục?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa tài khoản") { _, _ ->
                executor.execute {
                    try {
                        val r = api.request("delete_account", "POST")
                        runOnUiThread {
                            if (r.optBoolean("ok")) {
                                api.clearToken()
                                toast(r.optString("message", "Đã xóa tài khoản"))
                                showHome()
                            } else toast(r.optString("message", "Không thể xóa tài khoản"))
                        }
                    } catch (_: Exception) {
                        runOnUiThread { toast("Không kết nối được máy chủ") }
                    }
                }
            }.show()
    }

    private fun showProfile() {
        if (api.token() == null) { showLogin(); return }
        setup("Tài khoản")
        val loading = TextView(this).apply { text = "Đang tải tài khoản..."; textSize = 17f }
        root.addView(loading)
        executor.execute {
            try {
                val r = api.request("profile")
                runOnUiThread {
                    root.removeView(loading)
                    if (!r.optBoolean("ok")) { toast(r.optString("message", "Phiên đăng nhập hết hạn")); api.clearToken(); showLogin(); return@runOnUiThread }
                    val c = r.getJSONObject("data").getJSONObject("customer")
                    root.addView(TextView(this).apply { text = "Họ tên: ${c.optString("name")}\nSố điện thoại: ${c.optString("phone")}\nĐiểm tích luỹ: ${c.optInt("points")}"; textSize = 18f })
                    root.addView(button("📦 Đơn hàng") { showOrders() })
                    root.addView(button("🔐 Chính sách quyền riêng tư") { openUrl("https://com11h.com/privacy-policy.php") })
                    root.addView(button("🚪 Đăng xuất") { logoutAndHome() })
                    root.addView(button("⚠️ Xóa tài khoản") { deleteAccount() })
                    root.addView(button("← Trang chủ") { showHome() })
                }
            } catch (_: Exception) { runOnUiThread { toast("Không kết nối được máy chủ") } }
        }
    }

    private fun loadImageInto(url: String, imageView: ImageView) {
        if (url.isBlank()) return
        executor.execute {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.doInput = true
                val bitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                conn.disconnect()
                if (bitmap != null) runOnUiThread { imageView.setImageBitmap(bitmap) }
            } catch (_: Exception) { }
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) { toast("Không có liên kết thanh toán"); return }
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { toast("Thiết bị không có ứng dụng mở liên kết này") }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
