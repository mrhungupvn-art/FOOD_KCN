package com.com11h.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Lớp gọi API DUY NHẤT của app tới com11h.com/api/index.php — dùng cho đăng
 * nhập/đăng ký tài khoản, thực đơn, đặt hàng, thanh toán, quay số may mắn và
 * điểm tích luỹ. Toàn bộ dữ liệu app hiển thị (trừ giỏ hàng tạm trước khi đặt)
 * đều lấy trực tiếp từ đây, luôn khớp với tài khoản trên website.
 */
class AccountSync(context: Context) {
    companion object {
        private const val BASE_URL = "https://com11h.com/api/index.php"
        private const val PREFS = "com11h_secure"
        private const val TOKEN = "token"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun token(): String? = prefs.getString(TOKEN, null)
    fun isLoggedIn(): Boolean = !token().isNullOrBlank()
    fun saveToken(value: String) = prefs.edit().putString(TOKEN, value).apply()
    fun logout() = prefs.edit().remove(TOKEN).apply()

    /**
     * @param action    tên action (?action=...)
     * @param method    GET/POST
     * @param body      JSON body cho POST (null nếu không có)
     * @param headers   header bổ sung, vd "X-Idempotency-Key" cho create_order
     * @param query     tham số query bổ sung, vd "code" cho action=order
     */
    fun request(
        action: String,
        method: String = "GET",
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        query: Map<String, String> = emptyMap()
    ): JSONObject {
        val qs = StringBuilder("action=").append(URLEncoder.encode(action, "UTF-8"))
        query.forEach { (k, v) -> qs.append('&').append(URLEncoder.encode(k, "UTF-8")).append('=').append(URLEncoder.encode(v, "UTF-8")) }
        val url = URL("$BASE_URL?$qs")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 15000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            token()?.let { setRequestProperty("Authorization", "Bearer $it") }
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = try { JSONObject(text) } catch (_: Exception) {
                JSONObject().put("ok", false).put("message", "Máy chủ trả về dữ liệu không hợp lệ (HTTP $code).")
            }
            if (!json.has("http_code")) json.put("http_code", code)
            json
        } finally { c.disconnect() }
    }
}
